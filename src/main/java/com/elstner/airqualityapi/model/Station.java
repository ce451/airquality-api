package com.elstner.airqualityapi.model;

import com.elstner.airqualityapi.view.JsonViews;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(JsonViews.Basic.class)
    private Long id;

    @JsonView(JsonViews.Basic.class)
    private String name;

    @JsonView(JsonViews.Basic.class)
    private String ipAddress;

    @JsonView(JsonViews.Basic.class)
    private StationStatus status;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(JsonViews.Detailed.class)
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
