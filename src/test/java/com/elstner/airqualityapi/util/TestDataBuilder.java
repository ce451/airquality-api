package com.elstner.airqualityapi.util;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationGroup;
import com.elstner.airqualityapi.model.StationStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Test data builder utility for creating consistent test data across all tests.
 * Provides fluent builder methods for domain models with sensible defaults.
 */
public class TestDataBuilder {

    /**
     * Creates a Station builder with default test values.
     * Default values:
     * - ipAddress: 192.168.1.100
     * - name: Test Station
     * - status: ONLINE
     */
    public static StationBuilder aStation() {
        return new StationBuilder();
    }

    /**
     * Creates a Measurement builder with default test values.
     * Default values:
     * - temperature: 22.0°C
     * - humidity: 55.0%
     * - voltage: 3.3V
     * - timestamp: now (Europe/Vienna timezone)
     */
    public static MeasurementBuilder aMeasurement() {
        return new MeasurementBuilder();
    }

    /**
     * Creates a StationGroup builder with default test values.
     * Default values:
     * - displayName: Test Group
     * - displayOrder: 1
     * - active: true
     */
    public static StationGroupBuilder aStationGroup() {
        return new StationGroupBuilder();
    }

    public static class StationBuilder {
        private Long id;
        private String ipAddress = TestConstants.DEFAULT_IP_ADDRESS;
        private String name = TestConstants.DEFAULT_STATION_NAME;
        private StationStatus status = StationStatus.ONLINE;
        private Long stationGroupId;
        private Integer displayOrder;

        public StationBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public StationBuilder withIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public StationBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public StationBuilder withStatus(StationStatus status) {
            this.status = status;
            return this;
        }

        public StationBuilder withStationGroupId(Long stationGroupId) {
            this.stationGroupId = stationGroupId;
            return this;
        }

        public StationBuilder withDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public StationBuilder asNew() {
            this.status = StationStatus.NEW;
            this.name = "New Station";
            return this;
        }

        public StationBuilder asOffline() {
            this.status = StationStatus.OFFLINE;
            return this;
        }

        public StationBuilder asError() {
            this.status = StationStatus.ERROR;
            return this;
        }

        public Station build() {
            Station station = new Station();
            station.setId(id);
            station.setIpAddress(ipAddress);
            station.setName(name);
            station.setStatus(status);
            station.setStationGroupId(stationGroupId);
            station.setDisplayOrder(displayOrder);
            return station;
        }
    }

    public static class MeasurementBuilder {
        private UUID id;
        private Station station;
        private Float temperature = TestConstants.DEFAULT_TEMPERATURE;
        private Float humidity = TestConstants.DEFAULT_HUMIDITY;
        private Float voltage = TestConstants.DEFAULT_VOLTAGE;
        private ZonedDateTime timestamp = ZonedDateTime.now(ZoneId.of(TestConstants.TIMEZONE));

        public MeasurementBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public MeasurementBuilder withStation(Station station) {
            this.station = station;
            return this;
        }

        public MeasurementBuilder withTemperature(Float temperature) {
            this.temperature = temperature;
            return this;
        }

        public MeasurementBuilder withHumidity(Float humidity) {
            this.humidity = humidity;
            return this;
        }

        public MeasurementBuilder withVoltage(Float voltage) {
            this.voltage = voltage;
            return this;
        }

        public MeasurementBuilder withTimestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public MeasurementBuilder inPast(int hoursAgo) {
            this.timestamp = ZonedDateTime.now(ZoneId.of(TestConstants.TIMEZONE)).minusHours(hoursAgo);
            return this;
        }

        public MeasurementBuilder inFuture(int hoursAhead) {
            this.timestamp = ZonedDateTime.now(ZoneId.of(TestConstants.TIMEZONE)).plusHours(hoursAhead);
            return this;
        }

        public MeasurementBuilder cold() {
            this.temperature = 5.0f;
            this.humidity = 40.0f;
            return this;
        }

        public MeasurementBuilder hot() {
            this.temperature = 35.0f;
            this.humidity = 70.0f;
            return this;
        }

        public MeasurementBuilder lowBattery() {
            this.voltage = 2.8f;
            return this;
        }

        public Measurement build() {
            // Use the constructor to ensure absolute humidity is calculated
            Measurement measurement = new Measurement(station, temperature, humidity, voltage);
            if (id != null) {
                measurement.setId(id);
            }
            measurement.setTimestamp(timestamp);
            return measurement;
        }
    }

    public static class StationGroupBuilder {
        private Long id;
        private String displayName = TestConstants.DEFAULT_GROUP_NAME;
        private Integer displayOrder = 1;
        private Boolean active = true;

        public StationGroupBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public StationGroupBuilder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public StationGroupBuilder withDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public StationGroupBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public StationGroupBuilder inactive() {
            this.active = false;
            return this;
        }

        public StationGroup build() {
            StationGroup group = new StationGroup();
            group.setId(id);
            group.setDisplayName(displayName);
            group.setDisplayOrder(displayOrder);
            group.setActive(active);
            return group;
        }
    }
}
