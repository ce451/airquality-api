package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.repository.StationRepository;
import org.springframework.stereotype.Service;

@Service
public class StationServiceImpl implements StationService {
    private final StationRepository stationRepository;
    private static final String DEFAULT_STATION_NAME = "New Station";

    public StationServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }


    @Override
    public Station getOrCreateStation(String ipAddress) {
        return stationRepository.findByIpAddress(ipAddress)
                .orElseGet(() -> {
                    Station newStation = new Station();
                    newStation.setIpAddress(ipAddress);
                    newStation.setName(DEFAULT_STATION_NAME);
                    return stationRepository.save(newStation);
                });
    }
}
