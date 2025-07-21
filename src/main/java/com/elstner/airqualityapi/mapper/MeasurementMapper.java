package com.elstner.airqualityapi.mapper;

import com.elstner.airqualityapi.dto.MeasurementDto;
import com.elstner.airqualityapi.model.Measurement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MeasurementMapper {
    MeasurementDto ToDto(Measurement measurement);
}
