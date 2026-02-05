-- =========================================================================================
-- SCRIPT DE DATOS DE PRUEBA (VITSYNC)
-- =========================================================================================
-- ¡IMPORTANTE!: Este script asume que las tablas (Users, pacientes, medicos) YA EXISTEN.
-- Ejecuta primero la aplicación (VitSyncApplication) con el perfil 'dev' para que Hibernate las cree.
-- =========================================================================================

-- ... (Resto de comentarios anteriores) ...

-- 1. INSERTAR PACIENTE (En tabla base Users)
INSERT INTO Users (
    name, first_name, second_name, nif, email, password, gender, role, 
    birth_date, phone, address, post_code, country, is_verified, verification_code
) VALUES (
    'Paciente', 'Prueba', 'Uno', '11111111A', 'paciente@demo.com', 
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVwdFYiNu.k4Ni/kZ.yC.w.e', -- Hash de "demo"
    'MALE', 'PACIENTE', '1990-01-01', '600111222', 'Calle Falsa 123', '28001', 'Spain', TRUE, NULL
);

-- 2. INSERTAR PACIENTE (En tabla hija pacientes)
INSERT INTO pacientes (id, historial_clinico_id) 
VALUES (
    (SELECT id FROM Users WHERE nif='11111111A'), 
    'HIST-DEMO-001'
);


-- 3. INSERTAR MÉDICO (En tabla base Users)
INSERT INTO Users (
    name, first_name, second_name, nif, email, password, gender, role, 
    birth_date, phone, address, post_code, country, is_verified, verification_code
) VALUES (
    'Medico', 'Prueba', 'Dos', '22222222B', 'medico@demo.com', 
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVwdFYiNu.k4Ni/kZ.yC.w.e', 
    'FEMALE', 'PROFESIONAL', '1985-05-05', '600333444', 'Hospital Central', '28002', 'Spain', TRUE, NULL
);

-- 4. INSERTAR MÉDICO (En tabla hija medicos)
INSERT INTO medicos (id, numero_colegiado, activo) 
VALUES (
    (SELECT id FROM Users WHERE nif='22222222B'), 
    'MED-99999', 
    TRUE
);


-- 5. CREAR RELACIÓN (Asignar Médico al Paciente)
INSERT INTO paciente_medico (paciente_id, medico_id) 
VALUES (
    (SELECT id FROM Users WHERE nif='11111111A'), 
    (SELECT id FROM Users WHERE nif='22222222B')
);
