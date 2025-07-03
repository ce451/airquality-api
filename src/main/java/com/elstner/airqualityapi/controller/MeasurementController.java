package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.assembler.MeasurementModelAssembler;
import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.service.StationService;
import com.elstner.airqualityapi.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
public class MeasurementController {
    private final MeasurementRepository measurementRepository;

    private final MeasurementModelAssembler measurementModelAssembler;

    private final StationService stationService;

    public MeasurementController(MeasurementRepository measurementRepository, MeasurementModelAssembler measurementModelAssembler, StationService stationService) {
        this.measurementRepository = measurementRepository;
        this.measurementModelAssembler = measurementModelAssembler;
        this.stationService = stationService;
    }

    @GetMapping("/measurements")
    public CollectionModel<EntityModel<Measurement>> all() {
        var measurements = measurementRepository.findAll().stream()
                    .sorted(Comparator.comparing(Measurement::getTimestamp).reversed())
                    .map(measurementModelAssembler::toModel)
                    .collect(Collectors.toList());
        return  CollectionModel.of(measurements, linkTo(MeasurementController.class).withSelfRel());
    }

    @GetMapping("/measurements/{id}")
    public EntityModel<Measurement> one(@PathVariable UUID id) {
        var measurement = measurementRepository.findById(id).orElseThrow();
        return measurementModelAssembler.toModel(measurement);
    }

    @GetMapping("/stations/{stationId}/measurements")
    public CollectionModel<EntityModel<Measurement>> getMeasurementsByStation(@PathVariable Long stationId) {
//        var measurements = measurementRepository.findAll().stream()
//                .filter(measurement -> measurement.getStation().getId().equals(stationId))
//                .sorted(Comparator.comparing(Measurement::getTimestamp).reversed())
//                .map(measurementModelAssembler::toModel)
//                .collect(Collectors.toList());
//        return CollectionModel.of(measurements, linkTo(MeasurementController.class).withSelfRel());

        return null;

//        return CollectionModel.of(measurements, linkTo(MeasurementController.class).withSelfRel());
    }

    @PostMapping("/measurements")
    public ResponseEntity<?> create(@RequestBody Measurement measurement, HttpServletRequest request) {
        measurement.setTimestamp(LocalDateTime.now());

        String senderIp = HttpUtils.getClientIp(request);
        if (senderIp == null || senderIp.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                    .body(Problem.create()
                            .withTitle("Bad Request")
                            .withDetail("The provided IP address is invalid or not recognized."));
        }

        var station = stationService.getOrCreateStation(senderIp);
        measurement.setStation(station);

        measurementRepository.save(measurement);
        return ResponseEntity.ok(measurementModelAssembler.toModel(measurement));
    }


}
