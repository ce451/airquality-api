package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {

}
