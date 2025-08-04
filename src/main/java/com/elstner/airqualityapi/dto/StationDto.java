package com.elstner.airqualityapi.dto;

import com.elstner.airqualityapi.model.StationStatus;

public class StationDto {
    private Long id;
    private String name;
    private String ipAddress;
    private StationStatus status;
    private String roomGroup;
    private Integer roomGroupOrder;
    private Integer displayOrder;

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

    public void setRoomGroup(String roomGroup) { this.roomGroup = roomGroup; }

    public Integer getRoomGroupOrder() { return roomGroupOrder; }

    public void setRoomGroupOrder(Integer roomGroupOrder) { this.roomGroupOrder = roomGroupOrder; }

    public Integer getDisplayOrder() { return displayOrder; }

    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
