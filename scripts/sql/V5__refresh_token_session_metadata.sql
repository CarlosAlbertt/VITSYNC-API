-- ==============================================================
-- V5: Metadatos de sesión en refresh_tokens (Fase 2 — sesiones activas)
-- ==============================================================
-- Permiten mostrar las "sesiones activas" del usuario (dispositivo, IP y
-- última actividad) y cerrarlas individualmente o todas menos la actual.
-- En testing se crean solas (ddl-auto=update); en producción (validate)
-- ejecutar este script en Neon ANTES de desplegar.
-- ==============================================================

ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS ip_address   VARCHAR(45);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS user_agent   VARCHAR(512);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;
