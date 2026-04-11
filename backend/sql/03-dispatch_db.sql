-- ============================================================
-- dispatch_db Schema
-- 连接 dispatch_db 后执行：psql -U postgres -d dispatch_db -f 03-dispatch_db.sql
-- ============================================================

-- 车辆表
CREATE TABLE IF NOT EXISTS vehicles (
    id            BIGSERIAL     PRIMARY KEY,
    plate_no      VARCHAR(20)   NOT NULL UNIQUE,
    vehicle_type  VARCHAR(32)   NOT NULL,
    max_weight    DECIMAL(8,2)  NOT NULL,
    max_volume    DECIMAL(8,2)  NOT NULL,
    driver_id     BIGINT,
    status        VARCHAR(20)   NOT NULL DEFAULT 'idle'
                    CHECK (status IN ('idle','on_route','maintenance')),
    deleted       SMALLINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vehicles_status    ON vehicles(status);
CREATE INDEX IF NOT EXISTS idx_vehicles_driver_id ON vehicles(driver_id);

-- 调度方案表
CREATE TABLE IF NOT EXISTS dispatch_plans (
    id                    BIGSERIAL     PRIMARY KEY,
    plan_no               VARCHAR(32)   NOT NULL UNIQUE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'draft'
                            CHECK (status IN ('draft','confirmed','executing','completed','cancelled')),
    total_orders          INT           NOT NULL DEFAULT 0,
    total_routes          INT           NOT NULL DEFAULT 0,
    before_total_distance DECIMAL(12,2),
    after_total_distance  DECIMAL(12,2),
    before_vehicle_count  INT,
    after_vehicle_count   INT,
    algorithm_params      JSONB,
    created_by            BIGINT        NOT NULL,
    confirmed_at          TIMESTAMP,
    completed_at          TIMESTAMP,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plans_status     ON dispatch_plans(status);
CREATE INDEX IF NOT EXISTS idx_plans_created_by ON dispatch_plans(created_by);
CREATE INDEX IF NOT EXISTS idx_plans_created_at ON dispatch_plans(created_at);

-- 路线表
CREATE TABLE IF NOT EXISTS routes (
    id                  BIGSERIAL     PRIMARY KEY,
    plan_id             BIGINT        NOT NULL REFERENCES dispatch_plans(id),
    vehicle_id          BIGINT        REFERENCES vehicles(id),
    driver_id           BIGINT,
    status              VARCHAR(20)   NOT NULL DEFAULT 'offered'
                          CHECK (status IN ('offered','accepted','rejected','offer_exhausted','in_progress','completed')),
    estimated_distance  DECIMAL(10,2),
    estimated_duration  INT,
    actual_distance     DECIMAL(10,2),
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_routes_plan_id    ON routes(plan_id);
CREATE INDEX IF NOT EXISTS idx_routes_driver_id  ON routes(driver_id);
CREATE INDEX IF NOT EXISTS idx_routes_vehicle_id ON routes(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_routes_status     ON routes(status);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'routes'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'routes_status_check'
        ) THEN
            ALTER TABLE routes
                DROP CONSTRAINT routes_status_check;
        END IF;

        ALTER TABLE routes
            ALTER COLUMN vehicle_id DROP NOT NULL;

        ALTER TABLE routes
            ALTER COLUMN driver_id DROP NOT NULL;

        UPDATE routes
           SET status = 'accepted'
         WHERE status = 'assigned';

        ALTER TABLE routes
            ADD CONSTRAINT routes_status_check
            CHECK (status IN ('offered','accepted','rejected','offer_exhausted','in_progress','completed'));
    END IF;
END $$;

-- 路线候选司机表：一条 route 可对应多组 (vehicle_id, driver_id) 候选，用于后续抢单/拒单流程
CREATE TABLE IF NOT EXISTS route_offer_candidates (
    id                BIGSERIAL     PRIMARY KEY,
    route_id          BIGINT        NOT NULL REFERENCES routes(id),
    vehicle_id        BIGINT        NOT NULL REFERENCES vehicles(id),
    driver_id         BIGINT        NOT NULL,
    candidate_status  VARCHAR(20)   NOT NULL DEFAULT 'offered'
                        CHECK (candidate_status IN ('queued','offered','accepted','rejected','offer_exhausted')),
    offered_at        TIMESTAMP,
    responded_at      TIMESTAMP,
    display_order     INT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (route_id, vehicle_id, driver_id)
);

CREATE INDEX IF NOT EXISTS idx_route_offer_candidates_route_id ON route_offer_candidates(route_id);
CREATE INDEX IF NOT EXISTS idx_route_offer_candidates_driver_id ON route_offer_candidates(driver_id);
CREATE INDEX IF NOT EXISTS idx_route_offer_candidates_vehicle_id ON route_offer_candidates(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_route_offer_candidates_status ON route_offer_candidates(candidate_status);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'route_offer_candidates'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'route_offer_candidates_candidate_status_check'
        ) THEN
            ALTER TABLE route_offer_candidates
                DROP CONSTRAINT route_offer_candidates_candidate_status_check;
        END IF;

        ALTER TABLE route_offer_candidates
            ADD CONSTRAINT route_offer_candidates_candidate_status_check
            CHECK (candidate_status IN ('queued','offered','accepted','rejected','offer_exhausted'));
    END IF;
END $$;

-- 路线途经点表
CREATE TABLE IF NOT EXISTS route_stops (
    id          BIGSERIAL     PRIMARY KEY,
    route_id    BIGINT        NOT NULL REFERENCES routes(id),
    order_id    BIGINT        NOT NULL,
    stop_seq    INT           NOT NULL,
    stop_type   VARCHAR(10)   NOT NULL
                  CHECK (stop_type IN ('pickup','delivery')),
    address     VARCHAR(256)  NOT NULL,
    lng         DECIMAL(10,6) NOT NULL,
    lat         DECIMAL(10,6) NOT NULL,
    arrived_at  TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_route_stops_route   ON route_stops(route_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_order   ON route_stops(order_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_seq     ON route_stops(route_id, stop_seq);
