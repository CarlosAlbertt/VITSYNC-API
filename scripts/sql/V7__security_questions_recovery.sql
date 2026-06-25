-- ==============================================================
-- V7: Preguntas de recuperación + reseteo de contraseña — Fase 2
-- ==============================================================
-- security_question_1 / _2: enunciado de las preguntas (en claro; el usuario
--   debe poder verlas para responderlas en el flujo de recuperación).
-- security_answer_1 / _2: hash BCrypt de las respuestas (nunca en claro,
--   normalizadas trim+lowercase antes de hashear).
-- password_reset_code: hash BCrypt del código de un solo uso enviado por email.
-- password_reset_code_expiry: caducidad del código (10 min).
-- En testing se crean solas (ddl-auto=update); en producción (validate)
-- ejecutar este script en Neon ANTES de desplegar.
-- ==============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS security_question_1        VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS security_question_2        VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS security_answer_1          VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS security_answer_2          VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_code        VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_code_expiry TIMESTAMP;
