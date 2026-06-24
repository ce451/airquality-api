-- Identify a station by its stable device MAC instead of source IP, so a DHCP
-- lease change no longer forks the measurement history. Nullable for legacy
-- sensors that don't send the X-Station-Mac header yet; the partial unique index
-- enforces one station per MAC while still allowing many NULL rows.
ALTER TABLE station ADD COLUMN mac_address varchar(17);

CREATE UNIQUE INDEX uq_station_mac_address ON station (mac_address) WHERE mac_address IS NOT NULL;
