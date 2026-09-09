package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {
    List<Measurement> findByStationAndTimestampAfterOrderByTimestampDesc(Station station, ZonedDateTime timestamp);

    List<Measurement> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Newest measurement of every station in one repository round-trip. The
     * LATERAL join does an O(log n) top-1 index lookup per station via
     * idx_measurement_station_timestamp (a plain {@code DISTINCT ON} would scan
     * the whole index - Postgres has no loose index scan). Replaces the
     * previous one-repository-call-per-station loop. Note: Hibernate still
     * resolves each row's eager {@code station} reference with a small lookup
     * inside the same session; keep {@code Measurement.station} EAGER, the
     * caller reads {@code getStation().getId()} after the session closed.
     */
    @Query(value = """
            SELECT m.* FROM station s
            JOIN LATERAL (
                SELECT * FROM measurement
                WHERE station_id = s.id
                ORDER BY "timestamp" DESC
                LIMIT 1
            ) m ON true
            """, nativeQuery = true)
    List<Measurement> findLatestPerStation();

    @Modifying
    @Transactional
    @Query("DELETE FROM Measurement m WHERE m.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Thins out (decimates) measurements in the time band [start, end) by keeping only
     * the earliest measurement per (station, time-bucket) and deleting the rest.
     * The bucket size is {@code intervalSeconds}, aligned to the Unix epoch.
     * <p>
     * Idempotent: after a run each bucket holds a single row, so re-running over the
     * same band deletes nothing. {@code "timestamp"} is quoted because it is a Postgres
     * keyword.
     *
     * @param intervalSeconds target resolution in seconds (e.g. 30, 60, 300)
     * @param start           inclusive lower bound (older edge) of the band
     * @param end             exclusive upper bound (younger edge) of the band
     * @return number of deleted measurements
     */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM measurement
            WHERE id IN (
                SELECT id FROM (
                    SELECT id,
                           ROW_NUMBER() OVER (
                               PARTITION BY station_id,
                                            floor(extract(epoch from "timestamp") / :intervalSeconds)
                               ORDER BY "timestamp"
                           ) AS rn
                    FROM measurement
                    WHERE "timestamp" >= :start AND "timestamp" < :end
                ) ranked
                WHERE ranked.rn > 1
            )
            """, nativeQuery = true)
    int thinBucket(@Param("intervalSeconds") long intervalSeconds,
                   @Param("start") ZonedDateTime start,
                   @Param("end") ZonedDateTime end);

}
