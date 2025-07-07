package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByIpAddress(String ipAddress);
    Collection<Station> findByStatus(StationStatus status);
    @Query("""
            SELECT m FROM Measurement m
            WHERE m.station.id = :stationId
            ORDER BY m.timestamp DESC
            LIMIT 1
    """)
    List<Measurement> findLatestMeasurementByStation(Long stationId);
}
