package com.elstner.airqualityapi.dto;

import java.util.List;

public class StationGroupWithStationsDto {
    private String displayName;
    private Integer displayOrder;
    private Boolean active;
    private List<StationDto> stations;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<StationDto> getStations() {
        return stations;
    }

    public void setStations(List<StationDto> stations) {
        this.stations = stations;
    }
}
