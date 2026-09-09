# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot REST API for collecting and managing air quality measurements from IoT weather stations. The API automatically registers new stations by IP address, stores temperature/humidity/voltage data in PostgreSQL, and publishes real-time updates via WebSocket.

**Version:** 0.7.0 (managed in `application.properties`)

## Technology Stack

- **Framework:** Spring Boot 3.5.3
- **Java:** 24 (compiled to target 21)
- **Database:** PostgreSQL 15 (production), H2 (testing)
- **Migrations:** Flyway
- **Object Mapping:** MapStruct 1.5.5.Final
- **Real-time:** WebSocket with STOMP + SockJS
- **Build Tool:** Maven

## Development Commands

### Build and Run
```bash
# Build the project
./mvnw clean package

# Run the application (requires PostgreSQL running on localhost:5432)
./mvnw spring-boot:run

# Run tests
./mvnw test
```

### Docker
```bash
# Start PostgreSQL and API together
docker-compose up

# Build Docker image manually
docker build -t airquality-api .
```

The Docker setup includes:
- PostgreSQL database on port 5432 (credentials in `docker-compose.yml`)
- API on port 8080

## Architecture

### Core Domain Model

**Station** - Represents a physical weather station identified by IP address
- Auto-created when first measurement arrives from new IP
- Has status: NEW, ACTIVE, INACTIVE, FAILED
- Belongs to optional StationGroup with display ordering
- One-to-many relationship with Measurements

**Measurement** - Environmental sensor reading
- UUID primary key
- Stores temperature, humidity, voltage
- Auto-calculates absolute humidity on creation (constructor logic)
- Timestamp defaults to `ZonedDateTime.now()`; stored **UTC-naive** (`timestamp without time zone`), serialized to clients as `Europe/Vienna` via Jackson

**StationGroup** - Logical grouping of stations (e.g., rooms in a building)
- Display name and ordering for UI presentation
- Active/inactive flag

### Layered Architecture

**Controllers** (`controller/`) - REST endpoints
- `MeasurementController` - POST /measurements for IoT devices; GET /measurements is a newest-first dump capped via `?limit=` (default 1000, max 10000)
- `StationController` - Full CRUD + specialized endpoints:
  - `GET /stations/{id}/measurements?minutes=X&maxPoints=N` — time-window series; optional `maxPoints` downsamples server-side (evenly strided, newest+oldest kept, see `MeasurementSampler`)
  - `GET /stations/measurements?minutes=X&maxPoints=N` — **batch**: the same series for ALL stations in one response (used by the dashboard to avoid N per-card requests)
  - `GET /stations/latestMeasurement` — newest reading per station (single `DISTINCT ON` query)
- `StationGroupController` - Manage station groupings
- `VersionController` - Returns application version from properties

**Services** (`service/`)
- `StationServiceImpl` - Handles auto-registration: `getOrCreateStation(ipAddress)`
- `MeasurementPublisher` - Publishes new measurements to WebSocket topic `/topic/measurements`
- `MeasurementCleanupService` - `@Scheduled` daily 02:00; deletes measurements older than `measurement.retention.days` (30)
- `MeasurementThinningService` - three `@Scheduled` tiers (5 min / hourly / daily); cascaded downsampling of old measurements (see "Scheduled Data Lifecycle")

**Repositories** (`repository/`) - Spring Data JPA repositories
- Custom queries like `findByStationAndTimestampAfterOrderByTimestampDesc`
- `findLatestPerStation` — native `DISTINCT ON (station_id)` for the newest reading of every station in one query
- `deleteByTimestampBefore` (retention cleanup) and `thinBucket(intervalSeconds, start, end)` — a native windowed `DELETE` that keeps one row per `(station, time-bucket)` for downsampling

**Mappers** (`mapper/`) - MapStruct interfaces for entity ↔ DTO conversion
- Configured with `componentModel = "spring"` for dependency injection
- Handles complex mappings like `StationWithMeasurementsDto`

**DTOs** (`dto/`) - Data transfer objects
- Separate DTOs for different response shapes (e.g., `StationDto`, `StationWithMeasurementsDto`)
- `MeasurementPointDto` — compact chart point (no `id`/`voltage`, ~35% smaller) used for all measurement **lists**; `MeasurementDto` remains for single-measurement responses
- `MeasurementWithStationDto` used for WebSocket updates (shape unchanged, still carries `id`/`voltage`)
- Write endpoints (POST/PUT/PATCH `/stations`) return `StationDto`, never the entity (`Station.measurements` is additionally `@JsonIgnore`d)

### Key Workflow: Receiving Measurements

1. IoT device POSTs measurement data to `/measurements`
2. `MeasurementController` extracts client IP using `HttpUtils.getClientIp()`
3. `StationService.getOrCreateStation()` finds existing or creates NEW station
4. Measurement saved to database (absolute humidity calculated in constructor)
5. `MeasurementPublisher` broadcasts update to WebSocket subscribers at `/topic/measurements`

### Scheduled Data Lifecycle

`@EnableScheduling` is active on `AirQualityApiApplication`. Two scheduled tasks keep measurement volume in check (sensors POST every ~15s since the 2026-06-08 SHT3x changeover, so the table grows fast):

- **Cleanup** (`MeasurementCleanupService`, daily 02:00, cron `measurement.cleanup.cron`): hard-deletes measurements older than `measurement.retention.days` (default 30).
- **Thinning / downsampling** (`MeasurementThinningService`): progressively decimates older data — keeps one measurement per `(station, time-bucket)` and deletes the rest. Cascade (configurable via `measurement.thinning.tierN.{after-minutes,interval-seconds}`): >10 min → 30s, >1 h → 60s, >1 day → 300s. The last 10 minutes are untouched. Idempotent (re-running a band deletes nothing). Each tier has its **own schedule** (tier 1 every 5 min `measurement.thinning.cron`, tier 2 hourly `…tier2.cron`, tier 3 daily 03:23 `…tier3.cron`): the DELETE re-scans its whole band per run, so the wide bands must not run every 5 minutes (that caused sustained I/O and index bloat — 144 MB indexes over a 9.4 MB heap, observed 2026-09).
- **Ops note:** the constant delete/insert churn still bloats the B-tree indexes slowly; if `pg_indexes_size('measurement')` grows far beyond the heap size again, run `REINDEX TABLE CONCURRENTLY measurement`.

**Timestamp storage gotcha:** the `measurement.timestamp` column is `timestamp without time zone` storing **UTC-naive** values; the deployed API container runs in **UTC**, so `ZonedDateTime.now()` aligns with stored values. Time-window queries depend on this — compute boundaries in `ZonedDateTime`/SQL `now()` (UTC), not local wall-clock.

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`
- Currently on **V10** (indexes on `measurement(station_id, timestamp)` and `(timestamp)` — before V10 the only index was the PK on `id`)
- Notable migrations: V4 added absolute humidity, V5 added voltage, V8-V9 refactored grouping, V10 added measurement indexes
- The live DB's history is baselined (V1) through V9, so new migrations start at **V10**. **Do not edit already-applied migration files** (Flyway checksum validation) — only add new `V{n}__*.sql`.

**Important:** `spring.jpa.hibernate.ddl-auto=validate` in production - schema changes MUST use Flyway migrations.

### Testing

- Default tests run on **H2** (`src/test/resources/application.properties`, Flyway off, `ddl-auto=none`).
- `MeasurementThinningServiceTest` (Mockito) — covers the thinning service's cascade and band-boundary logic (3 contiguous bands, resolution 30/60/300s, exception resilience). Pure unit test, no DB.
- `MeasurementRepositoryThinningTest` — a `@DataJpaTest` slice test that runs the native `thinBucket` `DELETE` against a **real Postgres 15 via Testcontainers** (H2 can't faithfully run the windowed `extract(epoch …)` / `ROW_NUMBER()` query). Builds the schema with `ddl-auto=create-drop` (there is no `V1` file to migrate from scratch) and `hibernate.timezone.default_storage=NORMALIZE_UTC` to mirror prod's UTC-naive column. Guarded with `@Testcontainers(disabledWithoutDocker = true)`: it **skips** (never fails the build) when Docker is unavailable, and runs in CI (`ubuntu-latest` has Docker). Caveat: against very new local Docker daemons (API ≥ 1.40) the bundled docker-java may fail to negotiate and skip locally — the query logic is then verified directly with `docker run postgres:15`.

## Configuration

### Database Connection
Local development: `jdbc:postgresql://localhost:5432/airquality_db` (user: postgres)
Docker: Points to `db` service container

### WebSocket
- Endpoint: `/ws` with SockJS fallback
- Topic: `/topic/measurements` for real-time measurement broadcasts
- CORS: Allows all origins (`setAllowedOriginPatterns("*")`)

### Important Settings
- Timezone: `Europe/Vienna` for Jackson **serialization** only; measurements are **stored UTC-naive** and the container runs UTC (see "Timestamp storage gotcha")
- JPA: `show-sql=false` (was flooding prod logs — 4 INSERTs/min/station); `open-in-view=false` (connections are released before response serialization; controllers must only serialize DTOs, nothing lazy)
- HTTP compression: `server.compression.enabled=true` for JSON (~5x smaller measurement lists); nginx in the UI container compresses as well
- Flyway: **enabled** in `application.properties` (`spring.flyway.enabled=true`, `locations=classpath:db/migration`); history baselined through V9

## Code Patterns

### MapStruct Usage
All entity-to-DTO conversions use MapStruct mappers (auto-generated at compile time). When adding new DTOs:
1. Define interface in `mapper/` package
2. Annotate with `@Mapper(componentModel = "spring")`
3. Inject into controller/service as Spring bean

### IP Address Extraction
Always use `HttpUtils.getClientIp(request)` to handle X-Forwarded-For headers correctly.

### Measurement Validation
Temperature must be > -100°C, humidity must be > 0%. Validation in `MeasurementController.create()`.

## Notes

- HATEOAS was explicitly removed (see comment in `pom.xml`) - use simple JSON responses
- Absolute humidity calculation uses meteorological formula: `AH = 6.112 * e^((17.67 * T) / (T + 243.5)) * (RH / 100)`
- Station auto-registration creates stations with name "New Station" and status NEW
