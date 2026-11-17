-- V2__create_refresh_tokens.sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id  ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token     ON refresh_tokens(token);

