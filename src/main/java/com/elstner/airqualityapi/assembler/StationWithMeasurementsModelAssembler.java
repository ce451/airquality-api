//package com.elstner.airqualityapi.assembler;
//
//import com.elstner.airqualityapi.controller.MeasurementController;
//import com.elstner.airqualityapi.controller.StationController;
//import com.elstner.airqualityapi.model.*;
//import com.elstner.airqualityapi.dto.*;
//import org.springframework.hateoas.server.RepresentationModelAssembler;
//import org.springframework.stereotype.Component;
//
//import java.util.stream.Collectors;
//
//import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
//
////@Component
////public class StationWithMeasurementsModelAssembler implements RepresentationModelAssembler<Station, StationWithMeasurementsModel> {
////    @Override
////    public StationWithMeasurementsModel toModel(Station station) {
////        StationWithMeasurementsModel model = new StationWithMeasurementsModel();
////        model.setId(station.getId());
////        model.setName(station.getName());
////        model.setIpAddress(station.getIpAddress());
////        model.setStatus(station.getStatus());
////        model.setMeasurements(
////                station.getMeasurements().stream().map(measurement -> {
////                    MeasurementModel m = new MeasurementModel();
////                    m.setId(measurement.getId());
////                    m.setTemperature(measurement.getTemperature());
////                    m.setHumidity(measurement.getHumidity());
////                    m.setAbsoluteHumidity(measurement.getAbsoluteHumidity());
////                    m.setTimestamp(measurement.getTimestamp());
////                    m.add(linkTo(methodOn(MeasurementController.class).one(measurement.getId())).withSelfRel());
////                    return m;
////                }).collect(Collectors.toList())
////        );
////
////        model.add(linkTo(methodOn(StationController.class).one(station.getId())).withSelfRel());
////        model.add(linkTo(methodOn(StationController.class).all()).withRel("stations"));
////        return model;
////    }
////}
