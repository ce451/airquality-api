package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.dto.MeasurementWithStationDto;
import com.elstner.airqualityapi.mapper.MeasurementMapper;
import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.service.MeasurementPublisher;
import com.elstner.airqualityapi.service.StationService;
import com.elstner.airqualityapi.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class MeasurementController {
    private final MeasurementRepository measurementRepository;
    private final StationService stationService;
    private final MeasurementMapper measurementMapper;
    private final MeasurementPublisher measurementPublisher;

    public MeasurementController(MeasurementRepository measurementRepository,
                                 StationService stationService,
                                 MeasurementMapper measurementMapper,
                                 MeasurementPublisher measurementPublisher) {
        this.measurementRepository = measurementRepository;
        this.stationService = stationService;
        this.measurementMapper = measurementMapper;
        this.measurementPublisher = measurementPublisher;
    }

    /** Newest-first dump, capped: this used to load and sort the whole table in memory. */
    @GetMapping("/measurements")
    public ResponseEntity<?> all(@RequestParam(name = "limit", defaultValue = "1000") int limit) {
        int cappedLimit = Math.clamp(limit, 1, 10_000);
        var measurements = measurementRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, cappedLimit));
        return ResponseEntity.ok(measurementMapper.toDtoList(measurements));
    }

    @GetMapping("/measurements/{id}")
    public ResponseEntity<?> one(@PathVariable UUID id) {
        var measurement = measurementRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(measurementMapper.toDto(measurement));
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

        String mac = request.getHeader("X-Station-Mac");
        var station = stationService.getOrCreateStation(mac, senderIp);
        measurement.setStation(station);

        var newMeasurement = new Measurement(station, measurement.getTemperature(), measurement.getHumidity(), measurement.getVoltage());

        measurementRepository.save(newMeasurement);

        MeasurementWithStationDto webSocketUpdate = new MeasurementWithStationDto();
        webSocketUpdate.setId(newMeasurement.getId());
        webSocketUpdate.setStationId(station.getId());
        webSocketUpdate.setTemperature(newMeasurement.getTemperature());
        webSocketUpdate.setHumidity(newMeasurement.getHumidity());
        webSocketUpdate.setAbsoluteHumidity(newMeasurement.getAbsoluteHumidity());
        webSocketUpdate.setVoltage(newMeasurement.getVoltage());
        webSocketUpdate.setTimestamp(newMeasurement.getTimestamp());
        measurementPublisher.publishMeasurementUpdate(webSocketUpdate);

        return ResponseEntity.ok(measurementMapper.toDto(newMeasurement));
    }
}
