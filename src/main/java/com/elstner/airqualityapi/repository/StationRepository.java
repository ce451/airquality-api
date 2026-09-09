package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByIpAddress(String ipAddress);
    Optional<Station> findByMacAddress(String macAddress);
    Collection<Station> findByStatus(StationStatus status);
}
