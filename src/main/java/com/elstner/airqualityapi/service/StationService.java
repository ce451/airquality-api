package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Station;

public interface StationService {

    public Station getOrCreateStation(String ipAddress);
}
