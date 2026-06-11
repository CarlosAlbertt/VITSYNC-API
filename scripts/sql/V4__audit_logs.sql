-- ==============================================================
-- V4: Tabla de auditoría (Fase 3.1) — RGPD Art. 30 / Ley 41/2002
-- ==============================================================
-- Append-only: estas filas NO deben actualizarse ni borrarse en
-- operación normal (son evidencia legal). La anonimización RGPD solo
-- reescribe actor_nif por un seudónimo (pseudonymizeActor).
-- ==============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_nif   VARCHAR(64),
    action      VARCHAR(32) NOT NULL,
    target_id   VARCHAR(255),
    success     BOOLEAN     NOT NULL,
    ip_address  VARCHAR(45),
    details     TEXT,
    timestamp   TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor     ON audit_logs (actor_nif);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action    ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp);
