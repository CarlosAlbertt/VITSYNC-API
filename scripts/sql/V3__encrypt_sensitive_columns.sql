-- ==============================================================
-- V3: Ampliar columnas que pasan a almacenar texto cifrado (Fase 2.4)
-- ==============================================================
-- AES-256-GCM + base64 expande el tamaño: IV(12) + ciphertext + tag(16),
-- luego base64 (~+33%). Un VARCHAR(255) original deja de caber.
-- Las columnas TEXT no necesitan cambio.
--
-- ⚠️ MIGRACIÓN DE DATOS: este script SOLO ajusta el esquema. Los datos
-- existentes están en CLARO y el converter espera base64(IV||ciphertext);
-- al leerlos lanzará error de descifrado. Antes de desplegar:
--   1) Exportar las filas afectadas.
--   2) Ejecutar el migrador puntual (clase utilitaria que cifra y
--      reescribe cada valor con la ENCRYPTION_KEY de producción), o
--      vaciar las columnas si son datos de prueba.
-- En un entorno nuevo/sin datos clínicos, basta con este ALTER.
-- ==============================================================

ALTER TABLE pacientes ALTER COLUMN historial_clinico_id TYPE TEXT;
ALTER TABLE pacientes ALTER COLUMN grupo_sanguineo      TYPE TEXT;
ALTER TABLE pacientes ALTER COLUMN contacto_emergencia  TYPE TEXT;
-- alergias y condiciones_previas ya son TEXT.
-- informes.notas_personales y mensajes.content ya son TEXT.
