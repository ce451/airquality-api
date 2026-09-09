package com.elstner.airqualityapi.dto;

import com.elstner.airqualityapi.model.StationStatus;

import java.util.List;

public class StationWithMeasurementsDto {
    private Long id;
    private String name;
    private String ipAddress;
    private StationStatus status;
    private Long stationGroupId;
    private Integer displayOrder;
    private List<MeasurementPointDto> measurements;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public Long getStationGroupId() { return stationGroupId; }

    public void setStationGroupId(Long stationGroupId) { this.stationGroupId = stationGroupId; }

    public Integer getDisplayOrder() { return displayOrder; }

    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public List<MeasurementPointDto> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<MeasurementPointDto> measurements) {
        this.measurements = measurements;
    }
}
