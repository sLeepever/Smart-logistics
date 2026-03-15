-- ============================================================
-- user_db Schema
-- 连接 user_db 后执行：psql -U postgres -d user_db -f 01-user_db.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL     PRIMARY KEY,
    username    VARCHAR(64)   NOT NULL UNIQUE,
    password    VARCHAR(128)  NOT NULL,
    real_name   VARCHAR(64),
    phone       VARCHAR(20),
    role        VARCHAR(20)   NOT NULL
                  CHECK (role IN ('admin','dispatcher','driver')),
    status      SMALLINT      NOT NULL DEFAULT 1,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
