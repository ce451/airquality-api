//package com.elstner.airqualityapi.assembler;
//
//import com.elstner.airqualityapi.controller.MeasurementController;
//import com.elstner.airqualityapi.controller.StationController;
//import com.elstner.airqualityapi.dto.MeasurementModel;
//import com.elstner.airqualityapi.dto.StationWithMeasurementsModel;
//import com.elstner.airqualityapi.model.Measurement;
//import com.elstner.airqualityapi.model.Station;
//import org.springframework.hateoas.server.RepresentationModelAssembler;
//import org.springframework.stereotype.Component;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
//import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
////
////@Component
////public class StationWithOneMeasurementModelAssembler implements RepresentationModelAssembler<Station, StationWithMeasurementsModel> {
////    @Override
////    public StationWithMeasurementsModel toModel(Station station) {
////        StationWithMeasurementsModel model = new StationWithMeasurementsModel();
////        model.setId(station.getId());
////        model.setName(station.getName());
////        model.setIpAddress(station.getIpAddress());
////        model.setStatus(station.getStatus());
////        if (station.getMeasurements() != null && !station.getMeasurements().isEmpty()) {
////            var latest = station.getMeasurements().stream()
////                    .max(Comparator.comparing(Measurement::getTimestamp))
////                    .orElse(null);
////
////            if (latest != null) {
////                MeasurementModel m = new MeasurementModel();
////                m.setId(latest.getId());
////                m.setTemperature(latest.getTemperature());
////                m.setHumidity(latest.getHumidity());
////                m.setAbsoluteHumidity(latest.getAbsoluteHumidity());
////                m.setTimestamp(latest.getTimestamp());
////                m.add(linkTo(methodOn(MeasurementController.class).one(latest.getId())).withSelfRel());
////
////                model.setMeasurements(List.of(m));
////            }
////        }
////
////        model.add(linkTo(methodOn(StationController.class).one(station.getId())).withSelfRel());
////        model.add(linkTo(methodOn(StationController.class).all()).withRel("stations"));
////        return model;
////    }
////}
