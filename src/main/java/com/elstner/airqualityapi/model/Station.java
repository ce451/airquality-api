package com.elstner.airqualityapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String ipAddress;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StationStatus status;

    private Long stationGroupId;

    private Integer displayOrder;

    // @JsonIgnore: if a Station entity ever reaches Jackson directly, the lazy
    // collection must not be serialized (whole measurement history) or touched
    // outside a session (open-in-view is disabled).
    @JsonIgnore
    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Measurement> measurements = new ArrayList<>();

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

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
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

    public void setDisplayOrder(Integer order) { this.displayOrder = order; }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    public Station() {
    }

    public Station(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.status = StationStatus.NEW;
    }

    @Override
    public String toString() {
        return String.format("%s (%S)", name, ipAddress);
    }
}
