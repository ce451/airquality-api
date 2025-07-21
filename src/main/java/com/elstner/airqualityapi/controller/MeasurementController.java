package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.service.StationService;
import com.elstner.airqualityapi.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class MeasurementController {
    private final MeasurementRepository measurementRepository;
    private final StationService stationService;

    public MeasurementController(MeasurementRepository measurementRepository,
                                 StationService stationService) {
        this.measurementRepository = measurementRepository;
        this.stationService = stationService;
    }

    @GetMapping("/measurements")
    public ResponseEntity<?> all() {
        var measurements = measurementRepository.findAll().stream()
                .sorted(Comparator.comparing(Measurement::getTimestamp).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(measurements);
    }

    @GetMapping("/measurements/{id}")
    public ResponseEntity<?> one(@PathVariable UUID id) {
        var measurement = measurementRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(measurement);
    }

    @PostMapping("/measurements")
    public ResponseEntity<?> create(@RequestBody Measurement measurement, HttpServletRequest request) {
        if (measurement == null || measurement.getTemperature() == null || measurement.getHumidity() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("No measurement data provided");
        }

        if (measurement.getTemperature() <= -100 ||
                measurement.getHumidity() <= 0) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid measurement data provided");
        }

        String senderIp = HttpUtils.getClientIp(request);
        if (senderIp == null || senderIp.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("The provided IP address is invalid or not recognized.");
        }

        var station = stationService.getOrCreateStation(senderIp);
        measurement.setStation(station);

        var newMeasurement = new Measurement(station, measurement.getTemperature(), measurement.getHumidity());

        measurementRepository.save(newMeasurement);
        return ResponseEntity.ok(newMeasurement);
    }
}
