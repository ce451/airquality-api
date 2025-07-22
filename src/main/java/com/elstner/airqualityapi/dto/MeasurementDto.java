package com.elstner.airqualityapi.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MeasurementDto {
    private UUID id;
    private Float temperature;
    private Float humidity;
    private Float absoluteHumidity;
    private ZonedDateTime timestamp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
