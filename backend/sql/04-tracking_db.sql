-- ============================================================
-- tracking_db Schema
-- 连接 tracking_db 后执行：psql -U postgres -d tracking_db -f 04-tracking_db.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS location_records (
    id           BIGSERIAL      PRIMARY KEY,
    route_id     BIGINT         NOT NULL,
    driver_id    BIGINT         NOT NULL,
    lng          DECIMAL(10,6)  NOT NULL,
    lat          DECIMAL(10,6)  NOT NULL,
    speed        DECIMAL(5,1),
    heading      DECIMAL(5,1),
    recorded_at  TIMESTAMP      NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_location_route_id  ON location_records(route_id);
CREATE INDEX IF NOT EXISTS idx_location_driver_id ON location_records(driver_id);
CREATE INDEX IF NOT EXISTS idx_location_recorded  ON location_records(recorded_at);
