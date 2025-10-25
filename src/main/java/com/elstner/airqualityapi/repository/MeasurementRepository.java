package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
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

    @Modifying
    @Transactional
    @Query("DELETE FROM Measurement m WHERE m.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") ZonedDateTime cutoffDate);

}
