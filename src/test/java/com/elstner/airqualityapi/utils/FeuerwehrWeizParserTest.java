package com.elstner.airqualityapi.utils;

import com.elstner.airqualityapi.utils.FeuerwehrWeizParser.Reading;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FeuerwehrWeizParserTest {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

    // Shape as served by the page (2026-09-23), shortened.
    private static String page(String tempRows, String humRows) {
        return """
                <script>
                      function drawChart() {
                        var data_temperatur = google.visualization.arrayToDataTable([
                          ['Zeit', 'Temperatur'],
                %s
                        ]);
                        var data_luftfeuchtigkeit = google.visualization.arrayToDataTable([
                          ['Zeit', 'Luftfeuchtigkeit'],
                %s
                        ]);
                        var data_wind = google.visualization.arrayToDataTable([
                          ['Zeit', 'Wind'],
                ['23.09. 07:30',1.2]
                        ]);
                """.formatted(tempRows, humRows);
    }

    @Test
    void parsesAndMergesBothSeries() {
        String html = page(
                "['23.09. 07:20',8.7],['23.09. 07:25',-1.5],['23.09. 07:30',8.9]",
                "['23.09. 07:20',0.85],['23.09. 07:25',0.83],['23.09. 07:30',0.81]");
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 23, 7, 40, 0, 0, VIENNA);

        List<Reading> readings = FeuerwehrWeizParser.parse(html, now);

        assertThat(readings).hasSize(3);
        Reading last = readings.get(2);
        assertThat(last.timestamp()).isEqualTo(ZonedDateTime.of(2026, 9, 23, 7, 30, 0, 0, VIENNA));
        assertThat(last.temperature()).isEqualTo(8.9f);
        assertThat(last.humidity()).isCloseTo(81f, within(0.001f));
        assertThat(readings.get(1).temperature()).isEqualTo(-1.5f);
        assertThat(readings).extracting(Reading::timestamp).isSorted();
    }

    @Test
    void wallClockIsInterpretedAsViennaTime() {
        String html = page("['23.09. 07:30',8.9]", "['23.09. 07:30',0.81]");
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 23, 5, 40, 0, 0, ZoneId.of("UTC"));

        Reading r = FeuerwehrWeizParser.parse(html, now).getFirst();

        // CEST = UTC+2
        assertThat(r.timestamp().toInstant())
                .isEqualTo(ZonedDateTime.of(2026, 9, 23, 5, 30, 0, 0, ZoneId.of("UTC")).toInstant());
    }

    @Test
    void timesWithoutMatchingHumidityAreSkipped() {
        String html = page("['23.09. 07:25',8.8],['23.09. 07:30',8.9]", "['23.09. 07:30',0.81]");
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 23, 7, 40, 0, 0, VIENNA);

        assertThat(FeuerwehrWeizParser.parse(html, now)).hasSize(1);
    }

    @Test
    void newYearRowsOfDecemberGetPreviousYear() {
        String html = page("['31.12. 23:55',-3.0],['01.01. 00:05',-3.2]",
                "['31.12. 23:55',0.90],['01.01. 00:05',0.91]");
        ZonedDateTime now = ZonedDateTime.of(2027, 1, 1, 0, 15, 0, 0, VIENNA);

        List<Reading> readings = FeuerwehrWeizParser.parse(html, now);

        assertThat(readings).extracting(r -> r.timestamp().getYear()).containsExactly(2026, 2027);
    }

    @Test
    void changedLayoutYieldsNoReadings() {
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 23, 7, 40, 0, 0, VIENNA);
        assertThat(FeuerwehrWeizParser.parse("<html>neue Seite</html>", now)).isEmpty();
    }
}
