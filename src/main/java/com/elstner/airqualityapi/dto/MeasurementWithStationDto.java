package com.elstner.airqualityapi.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MeasurementWithStationDto {
    private UUID id;
    private Long stationId;
    private Float temperature;
    private Float humidity;
    private Float absoluteHumidity;
    private Float voltage;
    private ZonedDateTime timestamp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getStationId() { return stationId; }

    public void setStationId(Long stationId) { this.stationId = stationId; }

    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Float getHumidity() {
        return humidity;
    }

    public void setHumidity(Float humidity) {
        this.humidity = humidity;
    }

    public Float getAbsoluteHumidity() {
        return absoluteHumidity;
    }

    public void setAbsoluteHumidity(Float absoluteHumidity) {
        this.absoluteHumidity = absoluteHumidity;
    }

    public Float getVoltage() { return voltage; }

    public void setVoltage(Float voltage) { this.voltage = voltage; }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
