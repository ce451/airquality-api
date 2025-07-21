package com.elstner.airqualityapi.controller;

//import com.elstner.airqualityapi.dto.StationWithMeasurementsModel;
import com.elstner.airqualityapi.mapper.StationMapper;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@RestController
public class StationController {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final StationMapper stationMapper;


    public StationController(StationRepository stationRepository,
                             MeasurementRepository measurementRepository,
                             StationMapper stationMapper) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.stationMapper = stationMapper;
    }

    @GetMapping("/stations")
    public ResponseEntity<?> all() {
        var stations = stationMapper.toStationDtoList(stationRepository.findAll());
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/stations/new")
    public ResponseEntity<?> newStations() {
        var stations = stationMapper.toStationDtoList(new ArrayList<>(stationRepository.findByStatus(StationStatus.NEW)));
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/stations/{id}")
    public ResponseEntity<?> one(@PathVariable Long id) {
        var station = stationRepository.findById(id)
                .map(stationMapper::toStationDto)
                .orElse(null);
        if (station == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/problem+json")
                    .body(null);
        }

        return ResponseEntity.ok(station);
    }

    @GetMapping("/stations/latestMeasurement")
    public ResponseEntity<?> latestMeasurement() {
        var stationsWithLatestMeasurements = stationRepository.findAll().stream()
                .map(station -> {
                    var latestMeasurement = stationRepository.findLatestMeasurementByStation(station.getId());
                    return stationMapper.toDtoWithMeasurements(station, latestMeasurement);
                })
                .toList();
        return  ResponseEntity.ok(stationsWithLatestMeasurements);
    }

    @GetMapping("/stations/{id}/measurements")
    public ResponseEntity<?> getStationWithMeasurements(
            @PathVariable Long id,
            @RequestParam(name = "minutes", defaultValue = "60") long minutes) {

        Station station = stationRepository.findById(id)
                .orElse(null);
        if (station == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/problem+json")
                    .body("Station not found with id: " + id);
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);

        var measurements = measurementRepository
                .findByStationAndTimestampAfterOrderByTimestampDesc(station, cutoff);

//        return ResponseEntity.ok(new StationWithMeasurementsModel(station, measurements));
        return ResponseEntity.ok(stationMapper.toDtoWithMeasurements(station, measurements));
    }

    @PostMapping("/stations")
    public ResponseEntity<?> createStation(@RequestBody Station station) {
        if (station.getName() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/problem+json")
                    .body("no station provided");
        }

        stationRepository.save(station);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(station);
    }

    @PutMapping("/stations/{id}")
    public ResponseEntity<?> updateStation(@PathVariable Long id, @RequestBody Station stationUpdate) {
        var updatedStation = stationRepository.findById(id)
                .map(station -> {
                    station.setName(stationUpdate.getName());
                    station.setIpAddress(stationUpdate.getIpAddress());
                    station.setStatus(stationUpdate.getStatus());
                    return stationRepository.save(station);
                }).orElse(null);

        if (updatedStation == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        return ResponseEntity.ok(updatedStation);
    }

    @PatchMapping("/stations/{id}/status")
    public ResponseEntity<?> updateStationStatus(@PathVariable Long id, @RequestParam StationStatus status) {
        var updatedStation = stationRepository.findById(id)
                .map(station -> {
                    station.setStatus(status);
                    return stationRepository.save(station);
                })
                .orElse(null);

        if (updatedStation == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
        return ResponseEntity.ok(updatedStation);
    }

    @DeleteMapping("/stations/{id}")
    public ResponseEntity<?> deleteStation(@PathVariable Long id) {
        var station = stationRepository.findById(id)
                .orElse(null);

        if (station == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/problem+json")
                    .body("Station not found with id: " + id);
        } else {
            stationRepository.delete(station);
        }

        return ResponseEntity.noContent().build();
    }

}
