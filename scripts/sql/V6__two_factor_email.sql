-- ==============================================================
-- V6: Verificación en dos pasos por email (2FA) — Fase 2
-- ==============================================================
-- two_factor_enabled: el usuario activó el 2FA.
-- two_factor_code: hash (BCrypt) del código de un solo uso del login.
-- two_factor_code_expiry: caducidad del código (10 min).
-- En testing se crean solas (ddl-auto=update); en producción (validate)
-- ejecutar este script en Neon ANTES de desplegar.
-- ==============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled     BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_code        VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_code_expiry TIMESTAMP;
