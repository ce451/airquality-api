package com.elstner.airqualityapi.dto;

import com.elstner.airqualityapi.model.StationStatus;
import org.springframework.hateoas.RepresentationModel;
import com.elstner.airqualityapi.dto.MeasurementModel;

import java.util.List;

public class StationWithMeasurementsModel extends RepresentationModel<StationWithMeasurementsModel> {
    private Long id;
    private String name;
    private String ipAddress;
    private StationStatus status;
    private List<MeasurementModel> measurements;

    // Getter und Setter
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

    public List<MeasurementModel> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<MeasurementModel> measurements) {
        this.measurements = measurements;
    }
}
