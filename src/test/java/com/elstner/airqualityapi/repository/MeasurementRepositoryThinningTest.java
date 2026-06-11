package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link MeasurementRepository#thinBucket} against a real PostgreSQL 15
 * (the production engine). The query is Postgres-specific (windowed DELETE with
 * {@code ROW_NUMBER() OVER (PARTITION BY ...)} and {@code extract(epoch from ...)}), so H2
 * cannot validate it faithfully.
 * <p>
 * Guarded with {@code disabledWithoutDocker = true}: if Docker is unavailable the whole class
 * is skipped rather than failed, so it can never block the CI build / deploy.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // No V1 migration file exists (V1 was a Flyway baseline), so build the schema from
        // the JPA entities instead of running migrations from scratch.
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        // Mirror production: store the UTC instant in a timestamp-without-time-zone column.
        "spring.jpa.properties.hibernate.timezone.default_storage=NORMALIZE_UTC",
        "spring.jpa.show-sql=false"
})
@Testcontainers(disabledWithoutDocker = true)
class MeasurementRepositoryThinningTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    // 2025-01-01T00:00:00Z -> epoch 1_735_689_600, divisible by 30/60/300 so the second
    // offsets below map onto clean, predictable bucket boundaries.
    private static final ZonedDateTime BASE =
            Instant.ofEpochSecond(1_735_689_600L).atZone(ZoneOffset.UTC);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MeasurementRepository repository;

    @Test
    void keepsEarliestRowPerBucketAndDeletesTheRest() {
        Station a = persistStation("A");
        // Three 30s buckets, two points each -> keep 0,30,60 ; delete 15,45,75.
        for (long s : new long[]{0, 15, 30, 45, 60, 75}) {
            persistMeasurement(a, s);
        }
        em.flush();

        int deleted = repository.thinBucket(30, BASE.minusSeconds(1), BASE.plusSeconds(90));

        assertThat(deleted).isEqualTo(3);
        assertThat(remainingOffsets(a)).containsExactly(0L, 30L, 60L);
    }

    @Test
    void isIdempotent() {
        Station a = persistStation("A");
        for (long s : new long[]{0, 15, 30, 45}) {
            persistMeasurement(a, s);
        }
        em.flush();

        ZonedDateTime start = BASE.minusSeconds(1);
        ZonedDateTime end = BASE.plusSeconds(60);
        int first = repository.thinBucket(30, start, end);
        int second = repository.thinBucket(30, start, end);

        assertThat(first).isEqualTo(2);
        assertThat(second).isZero();
        assertThat(remainingOffsets(a)).containsExactly(0L, 30L);
    }

    @Test
    void thinsEachStationIndependently() {
        Station a = persistStation("A");
        Station b = persistStation("B");
        // Both stations have two points in the same 30s bucket.
        persistMeasurement(a, 0);
        persistMeasurement(a, 15);
        persistMeasurement(b, 0);
        persistMeasurement(b, 15);
        em.flush();

        int deleted = repository.thinBucket(30, BASE.minusSeconds(1), BASE.plusSeconds(30));

        assertThat(deleted).isEqualTo(2);
        assertThat(remainingOffsets(a)).containsExactly(0L);
        assertThat(remainingOffsets(b)).containsExactly(0L);
    }

    @Test
    void leavesRowsOutsideTheBandUntouched() {
        Station a = persistStation("A");
        persistMeasurement(a, -15); // before start -> untouched
        persistMeasurement(a, 0);   // in band, bucket 0 -> kept (earliest)
        persistMeasurement(a, 15);  // in band, bucket 0 -> deleted
        persistMeasurement(a, 60);  // == end (exclusive) -> untouched
        em.flush();

        int deleted = repository.thinBucket(30, BASE, BASE.plusSeconds(60));

        assertThat(deleted).isEqualTo(1);
        assertThat(remainingOffsets(a)).containsExactly(-15L, 0L, 60L);
    }

    // --- helpers ---------------------------------------------------------------

    private Station persistStation(String name) {
        Station s = new Station();
        s.setName(name);
        return em.persistAndFlush(s);
    }

    private void persistMeasurement(Station station, long offsetSeconds) {
        Measurement m = new Measurement();
        m.setStation(station);
        m.setTemperature(20f);
        m.setHumidity(50f);
        m.setTimestamp(BASE.plusSeconds(offsetSeconds));
        em.persist(m);
    }

    /** Re-reads from the DB (the native DELETE bypasses the persistence context) and returns
     *  the surviving measurements' offsets (in seconds) from {@link #BASE}, sorted. */
    private List<Long> remainingOffsets(Station station) {
        em.flush();
        em.clear();
        return repository.findAll().stream()
                .filter(m -> m.getStation().getId().equals(station.getId()))
                .map(m -> Duration.between(BASE, m.getTimestamp()).getSeconds())
                .sorted()
                .toList();
    }
}
