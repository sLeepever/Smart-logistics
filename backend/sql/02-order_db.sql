-- ============================================================
-- order_db Schema
-- 连接 order_db 后执行：psql -U postgres -d order_db -f 02-order_db.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL      PRIMARY KEY,
    order_no         VARCHAR(32)    NOT NULL UNIQUE,
    sender_name      VARCHAR(64)    NOT NULL,
    sender_phone     VARCHAR(20)    NOT NULL,
    sender_address   VARCHAR(256)   NOT NULL,
    sender_lng       DECIMAL(10,6)  NOT NULL,
    sender_lat       DECIMAL(10,6)  NOT NULL,
    receiver_name    VARCHAR(64)    NOT NULL,
    receiver_phone   VARCHAR(20)    NOT NULL,
    receiver_address VARCHAR(256)   NOT NULL,
    receiver_lng     DECIMAL(10,6)  NOT NULL,
    receiver_lat     DECIMAL(10,6)  NOT NULL,
    goods_name       VARCHAR(128),
    weight           DECIMAL(8,2),
    volume           DECIMAL(8,2),
    status           VARCHAR(20)    NOT NULL DEFAULT 'pending'
                       CHECK (status IN ('pending_review','pending','dispatched','in_progress',
                                         'completed','cancelled','exception')),
    remark           TEXT,
    creator_id       BIGINT         NOT NULL,
    deleted          SMALLINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_creator    ON orders(creator_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_order_no   ON orders(order_no);
