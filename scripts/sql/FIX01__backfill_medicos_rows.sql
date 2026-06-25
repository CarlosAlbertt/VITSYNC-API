-- ==============================================================
-- FIX01: Médicos huérfanos + columnas legacy NOT NULL en `medicos`
-- ==============================================================
-- Problema 1: se sembraron médicos insertándolos SOLO en `users`
--   (role='MEDICO'). Con la herencia JOINED (User -> Medico) un médico necesita
--   también una fila en `medicos` con el mismo id; si no, el repositorio de
--   médicos (FROM Medico m ...) no los carga y no aparecen en el cuadro médico.
-- Problema 2: la tabla `medicos` arrastra columnas legacy `nombre` y `apellidos`
--   (NOT NULL, sin default) que la entidad JPA actual NO mapea. Esto rompe tanto
--   este backfill como la creación de médicos por la API (POST /api/medicos),
--   porque Hibernate no rellena esas columnas.
--
-- Paso 1: relajar las columnas legacy a NULLABLE (las desvincula del problema y
--   permite que el alta por panel/API funcione). Son columnas no usadas por el
--   código actual (los datos del médico viven en `users`).
-- Paso 2: crear la fila que falta en `medicos` para cada usuario MEDICO sin ella.
--   Idempotente (NOT EXISTS). Especialidad sin asignar (NULL): se asigna después
--   desde el panel de admin.
-- Ejecutar en Neon (producción; y testing si también se sembraron médicos allí).
-- ==============================================================

-- Paso 1: columnas legacy ya no obligatorias
ALTER TABLE medicos ALTER COLUMN nombre    DROP NOT NULL;
ALTER TABLE medicos ALTER COLUMN apellidos DROP NOT NULL;

-- Paso 2: backfill de las filas que faltan
INSERT INTO medicos (id, numero_colegiado, activo, especialidad_id, created_at, updated_at)
SELECT u.id,
       'COL-' || u.id,   -- número de colegiado único derivado del id
       TRUE,             -- activo (para que aparezca en el cuadro médico)
       NULL,             -- especialidad: asignar luego
       NOW(),
       NOW()
FROM users u
WHERE u.role = 'MEDICO'
  AND NOT EXISTS (SELECT 1 FROM medicos m WHERE m.id = u.id);

-- Comprobación:
-- SELECT u.id, u.email, m.numero_colegiado, m.activo, m.especialidad_id
-- FROM users u JOIN medicos m ON m.id = u.id WHERE u.role = 'MEDICO' ORDER BY u.id;
