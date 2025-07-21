package com.elstner.airqualityapi.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MeasurementDto {
    private UUID id;
    private Double temperature;
    private Double humidity;
    private Double absoluteHumidity;
    private ZonedDateTime timestamp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getAbsoluteHumidity() {
        return absoluteHumidity;
    }

    public void setAbsoluteHumidity(Double absoluteHumidity) {
        this.absoluteHumidity = absoluteHumidity;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
