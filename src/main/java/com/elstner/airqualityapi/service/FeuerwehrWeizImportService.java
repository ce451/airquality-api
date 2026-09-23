package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.repository.StationRepository;
import com.elstner.airqualityapi.utils.FeuerwehrWeizParser;
import com.elstner.airqualityapi.utils.FeuerwehrWeizParser.Reading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Imports the public weather station of the Stadtfeuerwehr Weiz as an additional
 * (virtual) station. The page only offers HTML with the last ~24 h at 5-minute
 * resolution, so each run scrapes it and stores the readings newer than the
 * station's latest measurement - this also back-fills gaps after downtime.
 * <p>
 * The station has no IP/MAC; it is identified by the pseudo address
 * {@value #STATION_ADDRESS} in {@code station.ip_address}.
 */
@Service
@ConditionalOnProperty(name = "external.ff-weiz.enabled", havingValue = "true", matchIfMissing = true)
public class FeuerwehrWeizImportService {

    private static final Logger log = LoggerFactory.getLogger(FeuerwehrWeizImportService.class);

    static final String STATION_ADDRESS = "ext:ff-weiz";
    private static final String STATION_NAME = "Feuerwehr Weiz";

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementPublisher measurementPublisher;
    private final RestClient restClient;

    @Value("${external.ff-weiz.url:https://www.stadtfeuerwehr-weiz.at/service-und-sicherheit/wetterstation/}")
    private String url;

    public FeuerwehrWeizImportService(StationRepository stationRepository,
                                      MeasurementRepository measurementRepository,
                                      MeasurementPublisher measurementPublisher,
                                      RestClient.Builder restClientBuilder) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.measurementPublisher = measurementPublisher;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Scheduled(cron = "${external.ff-weiz.cron:0 2/10 * * * ?}")
    public void importMeasurements() {
        try {
            String html = restClient.get().uri(url).retrieve().body(String.class);
            if (html == null) {
                log.warn("FF Weiz import: empty response from {}", url);
                return;
            }

            List<Reading> readings = FeuerwehrWeizParser.parse(html, ZonedDateTime.now());
            if (readings.isEmpty()) {
                log.warn("FF Weiz import: no readings found - page layout changed?");
                return;
            }

            Station station = stationRepository.findByIpAddress(STATION_ADDRESS)
                    .orElseGet(() -> stationRepository.save(new Station(STATION_NAME, STATION_ADDRESS)));
            var latest = measurementRepository.findTopByStationOrderByTimestampDesc(station)
                    .map(m -> m.getTimestamp().toInstant())
                    .orElse(null);

            // Same zone as ZonedDateTime.now() in the POST path, so storage stays UTC-naive in prod.
            ZoneId storageZone = ZoneId.systemDefault();
            List<Measurement> newMeasurements = readings.stream()
                    .filter(r -> latest == null || r.timestamp().toInstant().isAfter(latest))
                    // same plausibility rule as MeasurementController.create()
                    .filter(r -> r.temperature() > -100 && r.humidity() > 0)
                    .map(r -> {
                        var measurement = new Measurement(station, r.temperature(), r.humidity(), null);
                        measurement.setTimestamp(r.timestamp().withZoneSameInstant(storageZone));
                        return measurement;
                    })
                    .toList();

            measurementRepository.saveAll(newMeasurements);
            newMeasurements.forEach(measurementPublisher::publishMeasurementUpdate);
            log.info("FF Weiz import: {} new of {} readings", newMeasurements.size(), readings.size());
        } catch (Exception e) {
            log.warn("FF Weiz import failed: {}", e.toString());
        }
    }
}
