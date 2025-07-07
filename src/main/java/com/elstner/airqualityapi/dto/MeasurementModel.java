package com.elstner.airqualityapi.dto;

import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.UUID;

public class MeasurementModel extends RepresentationModel<MeasurementModel> {
    private UUID id;
    private Float temperature;
    private Float humidity;
    private Float absoluteHumidity;
    private LocalDateTime timestamp;

    // Getter und Setter
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Float getAbsoluteHumidity() { return absoluteHumidity; }

    public void setAbsoluteHumidity(Float absoluteHumidity) { this.absoluteHumidity = absoluteHumidity; }
}
