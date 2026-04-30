-- ============================================================
-- SEED DATA — perfil dev — se ejecuta automáticamente al arrancar
-- Paciente de prueba → NIF: 12345678A  Contraseña: secret
-- Médico de prueba   → NIF: 87654321B  Contraseña: secret
-- ============================================================

-- 1. Especialidades (idempotente por unique en codigo)
INSERT INTO especialidades (nombre, codigo, descripcion, tipo, slug, icono, activo, created_at, updated_at)
VALUES
    ('Cardiología',      'CARDIO',  'Diagnóstico y tratamiento de enfermedades del corazón y sistema cardiovascular.', 'MEDICA',   'cardiologia',       'heart',       TRUE, NOW(), NOW()),
    ('Traumatología',    'TRAUMA',  'Tratamiento de lesiones y enfermedades del aparato locomotor.',                    'MEDICA',   'traumatologia',     'bone',        TRUE, NOW(), NOW()),
    ('Medicina General', 'MEDGEN',  'Atención primaria, seguimiento del estado de salud y derivación a especialistas.','GENERAL',  'medicina-general',  'stethoscope', TRUE, NOW(), NOW());

-- 2. Usuario base del médico
INSERT INTO Users (name, first_name, second_name, nif, email, password, gender, role,
                   birth_date, phone, address, post_code, country,
                   is_verified, two_factor_enabled, suspended)
VALUES ('Rodríguez', 'Carlos', 'López',
        '87654321B', 'carlos.rodriguez@vitsync.dev',
        '$2a$10$W2iXrpaGN9rxmWauQaSsMeF7h9ViTspHgIyWfiEjHYCrAs4/mZDJe',
        'HOMBRE', 'MEDICO',
        '1980-03-22', '612345678', 'Calle Medicina 10', '28046', 'España',
        TRUE, FALSE, FALSE);

-- 3. Entrada medicos (vinculada al usuario médico y a Cardiología)
INSERT INTO medicos (id, numero_colegiado, bio, activo, created_at, updated_at, especialidad_id)
SELECT u.id,
       '28-12345',
       'Cardiólogo con 15 años de experiencia en el Hospital Universitario de Madrid. Especialista en arritmias y cardiopatía isquémica.',
       TRUE, NOW(), NOW(),
       e.id
FROM Users u
         JOIN especialidades e ON e.codigo = 'CARDIO'
WHERE u.nif = '87654321B'
  AND NOT EXISTS (SELECT 1 FROM medicos m WHERE m.id = u.id);

-- 4. Usuario base del paciente de prueba
INSERT INTO Users (name, first_name, second_name, nif, email, password, gender, role,
                   birth_date, phone, address, post_code, country,
                   is_verified, two_factor_enabled, suspended)
VALUES ('García', 'Juan', 'Martínez',
        '12345678A', 'juan.garcia@test.dev',
        '$2a$10$W2iXrpaGN9rxmWauQaSsMeF7h9ViTspHgIyWfiEjHYCrAs4/mZDJe',
        'HOMBRE', 'PACIENTE',
        '1990-05-15', '600000001', 'Calle Mayor 1', '28001', 'España',
        TRUE, FALSE, FALSE);

-- 5. Entrada pacientes
INSERT INTO pacientes (id, historial_clinico_id, grupo_sanguineo, alergias, condiciones_previas, contacto_emergencia)
SELECT u.id,
       'HC-2024-001',
       'A+',
       'Penicilina',
       'Hipertensión leve (controlada con medicación)',
       'María García — 600000002'
FROM Users u
WHERE u.nif = '12345678A'
  AND NOT EXISTS (SELECT 1 FROM pacientes p WHERE p.id = u.id);

-- 6. Citas (2): una futura confirmada, una pasada completada
INSERT INTO citas (paciente_id, medico_id, fecha_hora, tipo, estado, enlace_videoconsulta)
SELECT p.id, m.id,
       '2026-05-12 10:30:00',
       'Telemedicina', 'CONFIRMADA',
       'https://meet.vitsync.es/sala-abc123'
FROM Users p
         JOIN Users m ON m.nif = '87654321B'
WHERE p.nif = '12345678A'
  AND NOT EXISTS (
    SELECT 1 FROM citas c WHERE c.paciente_id = p.id AND c.fecha_hora = '2026-05-12 10:30:00'
  );

INSERT INTO citas (paciente_id, medico_id, fecha_hora, tipo, estado, enlace_videoconsulta)
SELECT p.id, m.id,
       '2026-04-14 09:00:00',
       'Presencial', 'COMPLETADA',
       NULL
FROM Users p
         JOIN Users m ON m.nif = '87654321B'
WHERE p.nif = '12345678A'
  AND NOT EXISTS (
    SELECT 1 FROM citas c WHERE c.paciente_id = p.id AND c.fecha_hora = '2026-04-14 09:00:00'
  );

-- 7. Informes (2): analítica reciente y ECG anterior
INSERT INTO informes (paciente_id, medico_id, titulo, tipo, fecha, archivo_url, notas_personales, favorito)
SELECT p.id, m.id,
       'Analítica de Sangre — Abril 2026',
       'Analítica',
       '2026-04-14',
       NULL,
       'Resultados dentro de los rangos normales. Colesterol total en el límite superior (195 mg/dL). Revisar dieta.',
       TRUE
FROM Users p
         JOIN Users m ON m.nif = '87654321B'
WHERE p.nif = '12345678A'
  AND NOT EXISTS (
    SELECT 1 FROM informes i WHERE i.paciente_id = p.id AND i.titulo = 'Analítica de Sangre — Abril 2026'
  );

-- 7. Informes Médicos (Pacientes)
INSERT INTO informes (paciente_id, medico_id, titulo, tipo, fecha, archivo_url, notas_personales, favorito)
SELECT p.id, m.id,
       'Analítica de Sangre — Abril 2026',
       'Laboratorio',
       '2026-04-14',
       '/uploads/analitica.pdf',
       'Niveles de glucosa y colesterol elevados. Se recomienda dieta baja en azúcares.',
       TRUE
FROM Users p
         JOIN Users m ON m.nif = '87654321B'
WHERE p.nif = '12345678A'
  AND NOT EXISTS (SELECT 1 FROM informes i WHERE i.paciente_id = p.id AND i.titulo = 'Analítica de Sangre — Abril 2026');

INSERT INTO informes (paciente_id, medico_id, titulo, tipo, fecha, archivo_url, notas_personales, favorito)
SELECT p.id, m.id,
       'Electrocardiograma — Marzo 2026',
       'Diagnóstico',
       '2026-03-15',
       NULL,
       'Ritmo sinusal normal. Sin alteraciones significativas. Frecuencia cardíaca en reposo: 68 lpm.',
       FALSE
FROM Users p
         JOIN Users m ON m.nif = '87654321B'
WHERE p.nif = '12345678A'
  AND NOT EXISTS (SELECT 1 FROM informes i WHERE i.paciente_id = p.id AND i.titulo = 'Electrocardiograma — Marzo 2026');

INSERT INTO informes (paciente_id, medico_id, titulo, tipo, fecha, archivo_url, notas_personales, favorito)
SELECT p.id, m.id,
       'Ecografía Abdominal',
       'Imagen',
       '2026-02-10',
       NULL,
       'Órganos en tamaño y posición normal.',
       FALSE
FROM Users p
         JOIN Users m ON m.nif = '87654321B'
WHERE p.nif = '12345678A'
  AND NOT EXISTS (SELECT 1 FROM informes i WHERE i.paciente_id = p.id AND i.titulo = 'Ecografía Abdominal');

-- 8. Indicadores de Salud (para Cardio y Metabolismo del paciente 12345678A)
INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'CARDIO', 'Colesterol LDL', 195.0, 'mg/dL', '2026-04-14', 'ALERTA'
FROM Users u WHERE u.nif = '12345678A';

INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'CARDIO', 'Frecuencia cardíaca', 68.0, 'lpm', '2026-03-15', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'METABOLISMO', 'Glucosa en ayunas', 105.0, 'mg/dL', '2026-04-14', 'ALERTA'
FROM Users u WHERE u.nif = '12345678A';

INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'METABOLISMO', 'HbA1c', 5.4, '%', '2026-04-14', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

-- Datos históricos para gráficos
INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'CARDIO', 'Colesterol LDL', 180.0, 'mg/dL', '2026-03-10', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'METABOLISMO', 'Glucosa en ayunas', 95.0, 'mg/dL', '2026-03-10', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

-- ACTIVIDAD
INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'ACTIVIDAD', 'Pasos diarios', 8420.0, 'pasos', '2026-04-14', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

-- NUTRICION
INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'NUTRICION', 'Vitamina D', 18.0, 'ng/mL', '2026-04-14', 'ALERTA'
FROM Users u WHERE u.nif = '12345678A';

INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'NUTRICION', 'Hierro', 72.0, 'µg/dL', '2026-04-14', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

-- AUDICION
INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'AUDICION', 'Umbral auditivo izq.', 18.0, 'dB', '2026-04-14', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

-- PULMONES
INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'PULMONES', 'Saturación de O2', 98.0, '%', '2026-04-14', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';

INSERT INTO indicadores_salud (paciente_id, categoria, nombre, valor, unidad, fecha, estado)
SELECT u.id, 'PULMONES', 'FVC', 3.8, 'L', '2026-04-14', 'NORMAL'
FROM Users u WHERE u.nif = '12345678A';
