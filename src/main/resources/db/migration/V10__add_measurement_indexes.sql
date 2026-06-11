-- Supporting indexes for the measurement table.
-- Until now the only index was the primary key on (id); all time-window and
-- per-station queries did sequential scans, which degrades as the table grows.

-- Dashboard / detail queries: WHERE station_id = ? AND timestamp > ? ORDER BY timestamp DESC
CREATE INDEX IF NOT EXISTS idx_measurement_station_timestamp
    ON measurement (station_id, "timestamp" DESC);

-- Retention cleanup + scheduled thinning: time-window scans across all stations
CREATE INDEX IF NOT EXISTS idx_measurement_timestamp
    ON measurement ("timestamp");
