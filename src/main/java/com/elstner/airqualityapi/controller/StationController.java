package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.mapper.StationMapper;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
                .sorted((s1, s2) -> {
                    var m1 = s1.getMeasurements().isEmpty() ? null : s1.getMeasurements().get(0);
                    var m2 = s2.getMeasurements().isEmpty() ? null : s2.getMeasurements().get(0);

                    if (m1 == null && m2 == null) return 0;
                    if (m1 == null) return 1;
                    if (m2 == null) return -1;
                    return m2.getTimestamp().compareTo(m1.getTimestamp()); // Descending
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

        LocalDateTime localCutoff = LocalDateTime.now().minusMinutes(minutes);
        ZonedDateTime cutoff = localCutoff.atZone(ZoneId.systemDefault());

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
                .body(stationMapper.toStationDto(station));
    }

    @PutMapping("/stations/{id}")
    public ResponseEntity<?> updateStation(@PathVariable Long id, @RequestBody Station stationUpdate) {
        var updatedStation = stationRepository.findById(id)
                .map(station -> {
                    // change properties, if provided by RequestBody
                    if (stationUpdate.getName() != null) station.setName(stationUpdate.getName());
                    if (stationUpdate.getIpAddress() != null) station.setIpAddress(stationUpdate.getIpAddress());
                    if (stationUpdate.getStatus() != null) station.setStatus(stationUpdate.getStatus());
                    if (stationUpdate.getStationGroupId()!= null) station.setStationGroupId(stationUpdate.getStationGroupId());
                    if (stationUpdate.getDisplayOrder() != null) station.setDisplayOrder(stationUpdate.getDisplayOrder());

                    return stationRepository.save(station);
                }).orElse(null);

        if (updatedStation == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        return ResponseEntity.ok(stationMapper.toStationDto(updatedStation));
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
        return ResponseEntity.ok(stationMapper.toStationDto(updatedStation));
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
