package com.elstner.airqualityapi.utils;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts temperature/humidity series from the Stadtfeuerwehr Weiz weather page
 * (https://www.stadtfeuerwehr-weiz.at/service-und-sicherheit/wetterstation/).
 * <p>
 * The page embeds the last ~24 h as inline Google-Charts arrays, e.g.
 * {@code var data_temperatur = google.visualization.arrayToDataTable([ ['Zeit','Temperatur'], ['23.09. 07:30',8.9], ... ]);}.
 * Timestamps are Europe/Vienna wall-clock without a year; humidity is a fraction (0.85 = 85 %).
 */
public final class FeuerwehrWeizParser {

    public record Reading(ZonedDateTime timestamp, float temperature, float humidity) {}

    private static final ZoneId SOURCE_ZONE = ZoneId.of("Europe/Vienna");

    private static final Pattern ROW = Pattern.compile(
            "\\['(\\d{2})\\.(\\d{2})\\. (\\d{2}):(\\d{2})',\\s*(-?\\d+(?:\\.\\d+)?)\\]");

    private FeuerwehrWeizParser() {}

    /**
     * @param html page source
     * @param now  reference time used to infer the missing year
     * @return readings present in both series, oldest first; empty if the page layout changed
     */
    public static List<Reading> parse(String html, ZonedDateTime now) {
        Map<LocalDateTime, Float> temperatures = parseSeries(html, "data_temperatur", now);
        Map<LocalDateTime, Float> humidities = parseSeries(html, "data_luftfeuchtigkeit", now);

        // Keyed by instant: in the DST fall-back hour two wall-clock times map to the
        // same instant (earlier offset); the later row simply wins.
        Map<ZonedDateTime, Reading> readings = new TreeMap<>();
        temperatures.forEach((time, temperature) -> {
            Float fraction = humidities.get(time);
            if (fraction == null) return;
            ZonedDateTime timestamp = ZonedDateTime.of(time, SOURCE_ZONE);
            readings.put(timestamp, new Reading(timestamp, temperature, fraction * 100f));
        });
        return new ArrayList<>(readings.values());
    }

    private static Map<LocalDateTime, Float> parseSeries(String html, String variable, ZonedDateTime now) {
        Map<LocalDateTime, Float> series = new TreeMap<>();
        int start = html.indexOf("var " + variable + " ");
        if (start < 0) return series;
        int end = html.indexOf("]);", start);
        if (end < 0) return series;

        LocalDateTime localNow = now.withZoneSameInstant(SOURCE_ZONE).toLocalDateTime();
        Matcher m = ROW.matcher(html.substring(start, end));
        while (m.find()) {
            try {
                LocalDateTime time = LocalDateTime.of(localNow.getYear(),
                        Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
                // Around New Year the Dec-31 rows would land in the future -> previous year.
                if (time.isAfter(localNow.plusDays(1))) time = time.minusYears(1);
                series.put(time, Float.parseFloat(m.group(5)));
            } catch (DateTimeException | NumberFormatException e) {
                // e.g. 29.02. evaluated against a non-leap year: skip the row
            }
        }
        return series;
    }
}
