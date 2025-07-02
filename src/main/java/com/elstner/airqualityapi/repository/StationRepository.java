package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByIpAddress(String ipAddress);
}
