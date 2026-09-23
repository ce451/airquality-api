package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.repository.StationRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeuerwehrWeizImportServiceTest {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");
    private static final DateTimeFormatter ROW_TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm");

    private final StationRepository stationRepository = mock(StationRepository.class);
    private final MeasurementRepository measurementRepository = mock(MeasurementRepository.class);
    private final MeasurementPublisher measurementPublisher = mock(MeasurementPublisher.class);

    private HttpServer server;
    private volatile int status = 200;
    private volatile String body;

    private FeuerwehrWeizImportService service;
    private Station station;

    // Three readings 25/20/15 minutes ago, in the page's wall-clock format.
    private final ZonedDateTime t1 = ZonedDateTime.now(VIENNA).withSecond(0).withNano(0).minusMinutes(25);
    private final ZonedDateTime t2 = t1.plusMinutes(5);
    private final ZonedDateTime t3 = t1.plusMinutes(10);

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        body = """
                var data_temperatur = google.visualization.arrayToDataTable([
                  ['Zeit', 'Temperatur'],
                ['%1$s',10.0],['%2$s',11.0],['%3$s',12.0]
                ]);
                var data_luftfeuchtigkeit = google.visualization.arrayToDataTable([
                  ['Zeit', 'Luftfeuchtigkeit'],
                ['%1$s',0.80],['%2$s',0.70],['%3$s',0.60]
                ]);
                """.formatted(t1.format(ROW_TIME), t2.format(ROW_TIME), t3.format(ROW_TIME));

        service = new FeuerwehrWeizImportService(stationRepository, measurementRepository,
                measurementPublisher, RestClient.builder());
        ReflectionTestUtils.setField(service, "url", "http://127.0.0.1:" + server.getAddress().getPort() + "/");

        station = new Station("Feuerwehr Weiz", FeuerwehrWeizImportService.STATION_ADDRESS);
        station.setId(42L);
        when(stationRepository.findByIpAddress(FeuerwehrWeizImportService.STATION_ADDRESS))
                .thenReturn(Optional.of(station));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @SuppressWarnings("unchecked")
    private List<Measurement> savedMeasurements() {
        ArgumentCaptor<List<Measurement>> captor = ArgumentCaptor.forClass(List.class);
        verify(measurementRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void firstRunCreatesStationAndStoresAllReadings() {
        when(stationRepository.findByIpAddress(FeuerwehrWeizImportService.STATION_ADDRESS)).thenReturn(Optional.empty());
        when(stationRepository.save(any(Station.class))).thenReturn(station);
        when(measurementRepository.findTopByStationOrderByTimestampDesc(station)).thenReturn(Optional.empty());

        service.importMeasurements();

        verify(stationRepository).save(argThat(s ->
                FeuerwehrWeizImportService.STATION_ADDRESS.equals(s.getIpAddress())));
        List<Measurement> saved = savedMeasurements();
        assertThat(saved).extracting(Measurement::getTemperature).containsExactly(10f, 11f, 12f);
        assertThat(saved.get(0).getHumidity()).isEqualTo(80f, org.assertj.core.api.Assertions.within(0.001f));
        assertThat(saved.get(0).getAbsoluteHumidity()).isNotNull();
        assertThat(saved.get(0).getTimestamp().toInstant()).isEqualTo(t1.toInstant());
        assertThat(saved.get(0).getTimestamp().getZone()).isEqualTo(ZoneId.systemDefault());
        verify(measurementPublisher, times(3)).publishMeasurementUpdate(any(Measurement.class));
    }

    @Test
    void onlyReadingsNewerThanLatestAreStored() {
        Measurement latest = new Measurement(station, 11f, 70f, null);
        latest.setTimestamp(t2.withZoneSameInstant(ZoneId.of("UTC")));
        when(measurementRepository.findTopByStationOrderByTimestampDesc(station)).thenReturn(Optional.of(latest));

        service.importMeasurements();

        assertThat(savedMeasurements()).extracting(Measurement::getTemperature).containsExactly(12f);
        verify(stationRepository, never()).save(any());
        verify(measurementPublisher, times(1)).publishMeasurementUpdate(any(Measurement.class));
    }

    @Test
    void httpErrorIsSwallowedAndStoresNothing() {
        status = 503;

        service.importMeasurements();

        verifyNoInteractions(measurementRepository, measurementPublisher);
    }

    @Test
    void changedLayoutStoresNothing() {
        body = "<html>neue Seite</html>";

        service.importMeasurements();

        verifyNoInteractions(measurementRepository, measurementPublisher);
        verify(stationRepository, never()).save(any());
    }
}
