-- ==============================================================
-- FIX01: Backfill de filas en `medicos` para médicos huérfanos
-- ==============================================================
-- Problema: se sembraron médicos insertándolos SOLO en la tabla `users`
-- (role='MEDICO'). Con la herencia JOINED (User -> Medico), un médico necesita
-- también una fila en `medicos` con el mismo id; si no, el repositorio de
-- médicos (FROM Medico m ...) no los carga y no aparecen en el cuadro médico.
--
-- Este script crea la fila que falta en `medicos` para cada usuario MEDICO que
-- no la tenga. Es IDEMPOTENTE (NOT EXISTS) y deja la especialidad sin asignar
-- (NULL): se asigna después desde el panel de admin o con los UPDATE de abajo.
-- Ejecutar en Neon (testing y/o producción).
-- ==============================================================

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

-- Comprobación: debería listar todos los médicos ya con fila en `medicos`.
-- SELECT u.id, u.email, m.numero_colegiado, m.activo, m.especialidad_id
-- FROM users u JOIN medicos m ON m.id = u.id WHERE u.role = 'MEDICO' ORDER BY u.id;
