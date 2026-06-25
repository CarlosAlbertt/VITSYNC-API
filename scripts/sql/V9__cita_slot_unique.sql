-- ==============================================================
-- V9: Unicidad de hueco de cita (control de concurrencia)
-- ==============================================================
-- Impide DOS citas ACTIVAS (no canceladas) del mismo médico a la misma fecha/hora.
-- Es un índice ÚNICO PARCIAL: las citas canceladas quedan fuera, de modo que al
-- cancelar una cita su hueco vuelve a estar disponible. Las citas sin médico
-- ("cualquier profesional", medico_id NULL) no colisionan (NULL es distinto).
--
-- Esta es la garantía REAL contra la condición de carrera: si dos usuarios
-- reservan el mismo hueco a la vez, la BD acepta solo el primero y el segundo
-- recibe un error de integridad (la API lo traduce a 409).
--
-- JPA no puede crear índices parciales, así que hay que ejecutarlo a mano en
-- Neon (testing y producción). Si falla por "could not create unique index"
-- es que ya existen citas activas duplicadas: hay que cancelar/limpiar una antes.
-- ==============================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_citas_medico_fechahora_activa
    ON citas (medico_id, fecha_hora)
    WHERE estado <> 'Cancelada';
