package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.assembler.StationEntityAssembler;
import com.elstner.airqualityapi.assembler.StationModelAssembler;
import com.elstner.airqualityapi.assembler.StationWithMeasurementsModelAssembler;
import com.elstner.airqualityapi.dto.StationModel;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import com.elstner.airqualityapi.repository.StationRepository;
import com.elstner.airqualityapi.service.StationService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
public class StationController {

    private final StationRepository stationRepository;
    private final StationModelAssembler stationModelAssembler;
    private final StationWithMeasurementsModelAssembler stationWithMeasurementsModelAssembler;

    public StationController(StationRepository stationRepository,
                             StationModelAssembler stationModelAssembler,
                             StationWithMeasurementsModelAssembler stationWithMeasurementsModelAssembler) {
        this.stationRepository = stationRepository;
        this.stationModelAssembler = stationModelAssembler;
        this.stationWithMeasurementsModelAssembler = stationWithMeasurementsModelAssembler;
    }

    @GetMapping("/stations")
    public ResponseEntity<?> all() {
        var stations = stationRepository.findAll().stream()
                .map(stationModelAssembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(stations,
                linkTo(methodOn(StationController.class).all()).withSelfRel()));
    }

    @GetMapping("/stations/new")
    public ResponseEntity<?> newStations() {
        var stations = stationRepository.findByStatus(StationStatus.NEW).stream()
                .map(stationModelAssembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(stations,
                linkTo(methodOn(StationController.class).newStations()).withSelfRel()));

    }

    @GetMapping("/stations/{id}")
    public ResponseEntity<?> one(@PathVariable Long id) {
        var station = stationRepository.findById(id)
                .orElse(null);

        if (station == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/problem+json")
                    .body(null);
        }

        return ResponseEntity
                .ok()
                .body(stationModelAssembler.toModel(station));
    }

    @GetMapping("/stations/lastMeasurements")
    public ResponseEntity<?> lastMeasurements() {
        // get all stations associated with one measurement with the latest timestamp
        long id = 1;
        var station = stationRepository.findById(id)
                .orElse(null);
        if (station == null) {
            return  null;
        }

        return ResponseEntity
                .ok()
                .body(stationModelAssembler.toModel(station));
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
                .created(linkTo(methodOn(StationController.class).one(station.getId())).toUri())
                .body(stationModelAssembler.toModel(station));
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

        return ResponseEntity
                .ok()
                .body(stationModelAssembler.toModel(updatedStation)
                        .add(linkTo(methodOn(StationController.class).one(updatedStation.getId())).withSelfRel()));
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
        return ResponseEntity
                .ok()
                .body(stationModelAssembler.toModel(updatedStation)
                        .add(linkTo(methodOn(StationController.class).one(updatedStation.getId())).withSelfRel()));
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
