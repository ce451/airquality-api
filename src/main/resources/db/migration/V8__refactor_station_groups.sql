ALTER TABLE station
    DROP COLUMN room_group_order,
    DROP COLUMN room_group,
    ADD COLUMN station_group_id bigint

