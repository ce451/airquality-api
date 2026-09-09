package com.elstner.airqualityapi.mapper;

import com.elstner.airqualityapi.dto.MeasurementPointDto;
import com.elstner.airqualityapi.dto.StationDto;
import com.elstner.airqualityapi.dto.StationWithMeasurementsDto;
import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StationMapper {
    StationDto toStationDto(Station station);
    List<StationDto> toStationDtoList(List<Station> stations);

    MeasurementPointDto toMeasurementPointDto(Measurement measurement);

    @Mapping(target = "measurements", source = "measurements")
    StationWithMeasurementsDto toDtoWithMeasurements(Station station, List<Measurement> measurements);
}
