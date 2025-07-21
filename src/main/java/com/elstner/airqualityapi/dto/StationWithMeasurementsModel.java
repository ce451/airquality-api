package com.elstner.airqualityapi.dto;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;

import java.util.List;

public class StationWithMeasurementsModel {
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

    public StationWithMeasurementsModel() {}

    public StationWithMeasurementsModel(Station station, List<Measurement> measurements) {
        this.id = station.getId();
        this.name = station.getName();
        this.ipAddress = station.getIpAddress();
        this.status = station.getStatus();
        this.measurements = measurements.stream().map(measurement -> {
            MeasurementModel m = new MeasurementModel();
            m.setId(measurement.getId());
            m.setTemperature(measurement.getTemperature());
            m.setHumidity(measurement.getHumidity());
            m.setAbsoluteHumidity(measurement.getAbsoluteHumidity());
            m.setTimestamp(measurement.getTimestamp());
            return m;
        }).toList();
    }
}
