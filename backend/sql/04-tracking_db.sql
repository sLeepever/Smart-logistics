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

CREATE TABLE IF NOT EXISTS order_chat_conversations (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT    NOT NULL UNIQUE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted      SMALLINT  NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_chat_messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT       NOT NULL,
    sender_user_id   BIGINT       NOT NULL,
    sender_role      VARCHAR(20)  NOT NULL,
    content          VARCHAR(2000) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT fk_order_chat_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES order_chat_conversations(id)
);

CREATE INDEX IF NOT EXISTS idx_chat_conversations_order_id
    ON order_chat_conversations(order_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_id
    ON order_chat_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at
    ON order_chat_messages(created_at);
