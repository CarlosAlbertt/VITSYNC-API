-- ==============================================================
-- V2: Tabla de refresh tokens (Fase 2.3 del hardening)
-- ==============================================================
-- Ejecutar manualmente en Neon ANTES de desplegar la versión con
-- RS256 + refresh tokens (spring.jpa.hibernate.ddl-auto=validate
-- exige que la tabla exista).
-- ==============================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,           -- SHA-256 hex del token opaco
    expires_at  TIMESTAMP   NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
