package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.assembler.StationEntityAssembler;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import com.elstner.airqualityapi.repository.StationRepository;
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
    private final StationEntityAssembler stationEntityAssembler;

    public StationController(StationRepository stationRepository, StationEntityAssembler stationEntityAssembler) {
        this.stationRepository = stationRepository;
        this.stationEntityAssembler = stationEntityAssembler;
    }

    @GetMapping("/stations")
    public CollectionModel<EntityModel<Station>> all() {
        List<EntityModel<Station>> stations = stationRepository.findAll().stream()
                .map(stationEntityAssembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(stations,
                linkTo(methodOn(StationController.class).all()).withSelfRel());
    }

    @GetMapping("/stations/new")
    public CollectionModel<EntityModel<Station>> newStations() {
        List<EntityModel<Station>> stations = stationRepository.findByStatus(StationStatus.NEW).stream()
                .map(stationEntityAssembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(stations,
                linkTo(methodOn(StationController.class).newStations()).withSelfRel());

    }

    @GetMapping("/stations/{id}")
    public ResponseEntity<EntityModel<Station>> one(@PathVariable Long id) {
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
                .body(stationEntityAssembler.toModel(station));
    }

    @GetMapping("/stations/lastMeasurements")
    public EntityModel<Station> lastMeasurements() {
        // get all stations associated with one measurement with the latest timestamp
        long id = 1;
        var station = stationRepository.findById(id)
                .orElse(null);
        if (station == null) {
            return  null;
        }

        return stationEntityAssembler.toModel(station);
    }

    @PostMapping("/stations")
    public ResponseEntity<EntityModel<Station>> createStation(Station station) {
        stationRepository.save(station);
        return ResponseEntity
                .created(linkTo(methodOn(StationController.class).one(station.getId())).toUri())
                .body(stationEntityAssembler.toModel(station));
    }

    @PutMapping("/stations/{id}")
    public ResponseEntity<EntityModel<Station>> updateStation(@PathVariable Long id, @RequestBody Station stationUpdate) {
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
                .body(stationEntityAssembler.toModel(updatedStation)
                        .add(linkTo(methodOn(StationController.class).one(updatedStation.getId())).withSelfRel()));
    }

    @PatchMapping("/stations/{id}/status")
    public ResponseEntity<EntityModel<Station>> updateStationStatus(@PathVariable Long id, @RequestParam StationStatus status) {
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
                .body(stationEntityAssembler.toModel(updatedStation)
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
