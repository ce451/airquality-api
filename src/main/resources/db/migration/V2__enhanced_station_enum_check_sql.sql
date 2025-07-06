ALTER TABLE station DROP CONSTRAINT station_status_check;
ALTER TABLE station ADD CONSTRAINT station_status_check CHECK ((status >= 0 AND status <= 3));