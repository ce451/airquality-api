# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot REST API for collecting and managing air quality measurements from IoT weather stations. The API automatically registers new stations by IP address, stores temperature/humidity/voltage data in PostgreSQL, and publishes real-time updates via WebSocket.

**Version:** 0.5.1 (managed in `application.properties`)

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
- `MeasurementController` - POST /measurements for IoT devices, GET for retrieving data
- `StationController` - Full CRUD + specialized endpoints like `/stations/latestMeasurement`
- `StationGroupController` - Manage station groupings
- `VersionController` - Returns application version from properties

**Services** (`service/`)
- `StationServiceImpl` - Handles auto-registration: `getOrCreateStation(ipAddress)`
- `MeasurementPublisher` - Publishes new measurements to WebSocket topic `/topic/measurements`
- `MeasurementCleanupService` - `@Scheduled` daily 02:00; deletes measurements older than `measurement.retention.days` (30)
- `MeasurementThinningService` - `@Scheduled` every 5 min; cascaded downsampling of old measurements (see "Scheduled Data Lifecycle")

**Repositories** (`repository/`) - Spring Data JPA repositories
- Custom queries like `findByStationAndTimestampAfterOrderByTimestampDesc`
- `findLatestMeasurementByStation` for most recent reading
- `deleteByTimestampBefore` (retention cleanup) and `thinBucket(intervalSeconds, start, end)` — a native windowed `DELETE` that keeps one row per `(station, time-bucket)` for downsampling

**Mappers** (`mapper/`) - MapStruct interfaces for entity ↔ DTO conversion
- Configured with `componentModel = "spring"` for dependency injection
- Handles complex mappings like `StationWithMeasurementsDto`

**DTOs** (`dto/`) - Data transfer objects
- Separate DTOs for different response shapes (e.g., `StationDto`, `StationWithMeasurementsDto`)
- `MeasurementWithStationDto` used for WebSocket updates

### Key Workflow: Receiving Measurements

1. IoT device POSTs measurement data to `/measurements`
2. `MeasurementController` extracts client IP using `HttpUtils.getClientIp()`
3. `StationService.getOrCreateStation()` finds existing or creates NEW station
4. Measurement saved to database (absolute humidity calculated in constructor)
5. `MeasurementPublisher` broadcasts update to WebSocket subscribers at `/topic/measurements`

### Scheduled Data Lifecycle

`@EnableScheduling` is active on `AirQualityApiApplication`. Two scheduled tasks keep measurement volume in check (sensors POST every ~15s since the 2026-06-08 SHT3x changeover, so the table grows fast):

- **Cleanup** (`MeasurementCleanupService`, daily 02:00, cron `measurement.cleanup.cron`): hard-deletes measurements older than `measurement.retention.days` (default 30).
- **Thinning / downsampling** (`MeasurementThinningService`, every 5 min, cron `measurement.thinning.cron`): progressively decimates older data — keeps one measurement per `(station, time-bucket)` and deletes the rest. Cascade (configurable via `measurement.thinning.tierN.{after-minutes,interval-seconds}`): >10 min → 30s, >1 h → 60s, >1 day → 300s. The last 10 minutes are untouched. Idempotent (re-running a band deletes nothing). The **first** production run is a large one-off delete (~90% of rows) — take a `pg_dump` backup first and run `VACUUM (ANALYZE) measurement` afterward.

**Timestamp storage gotcha:** the `measurement.timestamp` column is `timestamp without time zone` storing **UTC-naive** values; the deployed API container runs in **UTC**, so `ZonedDateTime.now()` aligns with stored values. Time-window queries depend on this — compute boundaries in `ZonedDateTime`/SQL `now()` (UTC), not local wall-clock.

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`
- Currently on **V10** (indexes on `measurement(station_id, timestamp)` and `(timestamp)` — before V10 the only index was the PK on `id`)
- Notable migrations: V4 added absolute humidity, V5 added voltage, V8-V9 refactored grouping, V10 added measurement indexes
- The live DB's history is baselined (V1) through V9, so new migrations start at **V10**. **Do not edit already-applied migration files** (Flyway checksum validation) — only add new `V{n}__*.sql`.

**Important:** `spring.jpa.hibernate.ddl-auto=validate` in production - schema changes MUST use Flyway migrations.

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
- JPA: `show-sql=true` for debugging
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
