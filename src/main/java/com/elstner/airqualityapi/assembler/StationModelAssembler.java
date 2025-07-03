package com.elstner.airqualityapi.assembler;

import com.elstner.airqualityapi.controller.StationController;
import com.elstner.airqualityapi.model.Station;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class StationModelAssembler implements RepresentationModelAssembler<Station, EntityModel<Station>> {
    @Override
    public EntityModel<Station> toModel(Station station) {
        return EntityModel.of(station,
                linkTo(methodOn(StationController.class).one(station.getId())).withSelfRel(),
                linkTo(methodOn(StationController.class).all()).withRel("stations"),
                linkTo(methodOn(StationController.class).newStations()).withRel("newStations"));
    }
}
