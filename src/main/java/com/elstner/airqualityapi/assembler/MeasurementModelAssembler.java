package com.elstner.airqualityapi.assembler;

import com.elstner.airqualityapi.controller.MeasurementController;
import com.elstner.airqualityapi.dto.MeasurementModel;
import com.elstner.airqualityapi.model.Measurement;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MeasurementModelAssembler implements RepresentationModelAssembler<Measurement, EntityModel<MeasurementModel>> {
    @Override
    public EntityModel<MeasurementModel> toModel(Measurement entity) {
        var model = new MeasurementModel();
        model.setId(entity.getId());
        model.setTemperature(entity.getTemperature());
        model.setHumidity(entity.getHumidity());
        model.setAbsoluteHumidity(entity.getAbsoluteHumidity());
        model.setTimestamp(entity.getTimestamp());
        model.add(linkTo(methodOn(MeasurementController.class).one(entity.getId())).withSelfRel());
        return EntityModel.of(model);
    }
}
