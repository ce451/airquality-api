package com.elstner.airqualityapi.model;

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

    private StationStatus status;

    private String roomGroup;

    private Integer roomGroupOrder;

    private Integer displayOrder;

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

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public String getRoomGroup() { return roomGroup; }

    public void setRoomGroup(String group) { this.roomGroup = group; }

    public Integer getRoomGroupOrder() { return roomGroupOrder; }

    public void setRoomGroupOrder(Integer roolGroupOrder) { this.roomGroupOrder = roolGroupOrder; }

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
