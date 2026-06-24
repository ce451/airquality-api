-- Store StationStatus as its enum NAME instead of the JPA-default ordinal.
-- The old numeric check constraint capped status at 3, so setting a station to
-- MAINTENANCE (ordinal 4) threw a constraint violation. Convert the column to
-- varchar and map the existing ordinals to their names, then re-add a check over
-- the valid names. Matches @Enumerated(EnumType.STRING) on Station.status.
ALTER TABLE station DROP CONSTRAINT station_status_check;

ALTER TABLE station ALTER COLUMN status TYPE varchar(20)
    USING (CASE status
        WHEN 0 THEN 'ONLINE'
        WHEN 1 THEN 'OFFLINE'
        WHEN 2 THEN 'ERROR'
        WHEN 3 THEN 'NEW'
        WHEN 4 THEN 'MAINTENANCE'
        ELSE 'NEW'
    END);

ALTER TABLE station ADD CONSTRAINT station_status_check
    CHECK (status IN ('ONLINE', 'OFFLINE', 'ERROR', 'NEW', 'MAINTENANCE'));
