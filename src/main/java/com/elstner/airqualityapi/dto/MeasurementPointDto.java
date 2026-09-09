package com.elstner.airqualityapi.dto;

import java.time.ZonedDateTime;

/**
 * Compact measurement representation for time-series responses (charts).
 * Deliberately omits {@code id} and {@code voltage}: neither is consumed by any
 * chart client, and together they make up ~35% of the uncompressed payload
 * (the UUID alone is 38 bytes per point).
 */
public class MeasurementPointDto {
    private Float temperature;
    private Float humidity;
    private Float absoluteHumidity;
    private ZonedDateTime timestamp;

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
