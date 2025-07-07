package com.elstner.airqualityapi.assembler;

import com.elstner.airqualityapi.controller.MeasurementController;
import com.elstner.airqualityapi.model.Measurement;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MeasurementEntityAssembler implements RepresentationModelAssembler<Measurement, EntityModel<Measurement>> {


    @Override
    public EntityModel<Measurement> toModel(Measurement measurement) {
        return EntityModel.of(measurement,
                linkTo(methodOn(MeasurementController.class).one(measurement.getId())).withSelfRel(),
                linkTo(methodOn(MeasurementController.class).all()).withRel("all"));
    }
}
