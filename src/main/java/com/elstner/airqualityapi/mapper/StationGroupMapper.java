package com.elstner.airqualityapi.mapper;

import com.elstner.airqualityapi.dto.StationGroupDto;
import com.elstner.airqualityapi.dto.StationGroupWithStationsDto;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StationGroupMapper {
    StationGroupDto toStationGroupDto(StationGroup stationGroup);
    List<StationGroupDto> toStationGroupDtoList(List<StationGroup> stationGroups);

    @Mapping(target = "stations", source = "stations")
    StationGroupWithStationsDto toStationGroupWithStationsDto(StationGroup stationGroup, List<Station> stations);

}
