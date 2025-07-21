//package com.elstner.airqualityapi.assembler;
//
//import com.elstner.airqualityapi.controller.StationController;
//import com.elstner.airqualityapi.dto.StationModel;
//import com.elstner.airqualityapi.model.Station;
//import org.springframework.hateoas.EntityModel;
//import org.springframework.hateoas.server.RepresentationModelAssembler;
//import org.springframework.stereotype.Component;
//
//import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
//
////@Component
////public class StationModelAssembler implements RepresentationModelAssembler<Station, EntityModel<StationModel>> {
////    @Override
////    public EntityModel<StationModel> toModel(Station station) {
////        StationModel model = new StationModel();
////        model.setId(station.getId());
////        model.setName(station.getName());
////        model.setIpAddress(station.getIpAddress());
////        model.setStatus(station.getStatus());
////
////        model.add(linkTo(methodOn(StationController.class).one(station.getId())).withSelfRel());
////        model.add(linkTo(methodOn(StationController.class).all()).withRel("stations"));
////        var em = EntityModel.of(model);
////        return em;
////    }
////}
