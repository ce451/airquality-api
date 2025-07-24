package com.elstner.airqualityapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
public class Measurement {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Station station;

    private Float temperature;
    private Float humidity;
    private Float absoluteHumidity;
    private Float voltage;
    private ZonedDateTime timestamp = ZonedDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
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

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Float getAbsoluteHumidity() {
        return absoluteHumidity;
    }

    public void setAbsoluteHumidity(Float absoluteHumidity) {
        this.absoluteHumidity = absoluteHumidity;
    }

    public Float getVoltage() { return voltage; }

    public void setVoltage(Float voltage) { this.voltage = voltage; }

    public Measurement() {}

    public Measurement(Station station, Float temperature, Float humidity, Float voltage) {
        this.station = station;
        this.temperature = temperature;
        this.humidity = humidity;
        this.voltage = voltage;

        // Calculate absolute humidity using the formula:
        // AH = 6.112 * e^((17.67 * T) / (T + 243.5)) * (RH / 100)
        if (this.temperature != null && this.humidity != null) {
            double e = 6.112 * Math.exp((17.67 * temperature) / (temperature + 243.5)) * (humidity / 100);
            this.absoluteHumidity = (float) e;
        }
    }
}
