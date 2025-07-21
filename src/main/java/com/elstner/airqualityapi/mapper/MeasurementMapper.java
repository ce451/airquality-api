package com.elstner.airqualityapi.mapper;

import com.elstner.airqualityapi.dto.MeasurementDto;
import com.elstner.airqualityapi.model.Measurement;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MeasurementMapper {
    MeasurementDto toDto(Measurement measurement);
     List<MeasurementDto> toDtoList(List<Measurement> measurements);
}
