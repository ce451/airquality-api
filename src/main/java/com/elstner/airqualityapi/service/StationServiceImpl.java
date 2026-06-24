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
    public Station getOrCreateStation(String macAddress, String ipAddress) {
        // Legacy sensors that don't send the X-Station-Mac header keep the old
        // IP-based identity.
        if (macAddress == null || macAddress.isBlank()) {
            return getOrCreateByIp(ipAddress);
        }

        // 1. Known device: use its station, refreshing the last-seen IP for reference.
        var byMac = stationRepository.findByMacAddress(macAddress);
        if (byMac.isPresent()) {
            Station station = byMac.get();
            if (ipAddress != null && !ipAddress.equals(station.getIpAddress())) {
                station.setIpAddress(ipAddress);
                return stationRepository.save(station);
            }
            return station;
        }

        // 2. First MAC-tagged POST from a device that already exists by IP: adopt
        //    that station and pin the MAC, so the existing history isn't forked.
        if (ipAddress != null && !ipAddress.isBlank()) {
            var byIp = stationRepository.findByIpAddress(ipAddress);
            if (byIp.isPresent() && byIp.get().getMacAddress() == null) {
                Station station = byIp.get();
                station.setMacAddress(macAddress);
                return stationRepository.save(station);
            }
        }

        // 3. Genuinely new device.
        Station newStation = new Station(DEFAULT_STATION_NAME, ipAddress);
        newStation.setMacAddress(macAddress);
        return stationRepository.save(newStation);
    }

    private Station getOrCreateByIp(String ipAddress) {
        return stationRepository.findByIpAddress(ipAddress)
                .orElseGet(() -> stationRepository.save(new Station(DEFAULT_STATION_NAME, ipAddress)));
    }
}
