package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para las entidades del modelo de datos.
 *
 * Verifica que:
 * - La herencia JPA (User → Paciente / Medico) funciona correctamente
 * - Los campos se asignan y recuperan sin errores
 * - Las relaciones entre entidades se establecen correctamente
 */
@DisplayName("Modelo de Datos — Entidades JPA")
class EntityModelTest {

    // ─── User ────────────────────────────────────────────────────────

    @Test
    @DisplayName("User: todos los campos se asignan correctamente")
    void user_allFieldsAreSetAndRetrieved() {
        User user = new User();
        user.setId(1L);
        user.setName("Carlos");
        user.setFirstName("Albert");
        user.setSecondName("García");
        user.setNif("12345678A");
        user.setEmail("carlos@vitsync.es");
        user.setPassword("hashedPassword");
        user.setGender(Gender.HOMBRE);
        user.setRole(Role.PACIENTE);
        user.setBirthDate(LocalDate.of(1990, 5, 15));
        user.setPhone("612345678");
        user.setAddress("Calle Mayor 1");
        user.setPostCode("46001");
        user.setCountry("España");
        user.setVerified(true);

        assertEquals("Carlos", user.getName());
        assertEquals("12345678A", user.getNif());
        assertEquals(LocalDate.of(1990, 5, 15), user.getBirthDate());
        assertTrue(user.isVerified());
    }

    @Test
    @DisplayName("User: birthDate es LocalDate, no String")
    void user_birthDateIsLocalDate() {
        User user = new User();
        user.setBirthDate(LocalDate.of(2000, 12, 25));

        assertInstanceOf(LocalDate.class, user.getBirthDate(),
                "birthDate debe ser de tipo LocalDate");
        assertEquals(2000, user.getBirthDate().getYear());
        assertEquals(12, user.getBirthDate().getMonthValue());
        assertEquals(25, user.getBirthDate().getDayOfMonth());
    }

    // ─── Paciente (herencia) ─────────────────────────────────────────

    @Test
    @DisplayName("Paciente hereda todos los campos de User")
    void paciente_inheritsUserFields() {
        Paciente paciente = new Paciente();
        paciente.setName("María");
        paciente.setRole(Role.PACIENTE);
        paciente.setBirthDate(LocalDate.of(1985, 3, 10));
        paciente.setGrupoSanguineo("A+");
        paciente.setAlergias("Penicilina");

        // Campos heredados de User
        assertEquals("María", paciente.getName());
        assertEquals(Role.PACIENTE, paciente.getRole());

        // Campos propios de Paciente
        assertEquals("A+", paciente.getGrupoSanguineo());
        assertEquals("Penicilina", paciente.getAlergias());

        // Paciente ES un User (polimorfismo)
        assertInstanceOf(User.class, paciente,
                "Paciente debe ser instancia de User (herencia JOINED)");
    }

    // ─── Medico (herencia) ───────────────────────────────────────────

    @Test
    @DisplayName("Medico hereda de User y tiene campos propios")
    void medico_inheritsUserAndHasOwnFields() {
        Medico medico = new Medico();
        medico.setName("Pablo");
        medico.setRole(Role.MEDICO);
        medico.setNumeroColegiado("COL-12345");
        medico.setBio("Especialista en traumatología");
        medico.setActivo(true);

        assertEquals("Pablo", medico.getName());
        assertEquals("COL-12345", medico.getNumeroColegiado());
        assertTrue(medico.getActivo());
        assertInstanceOf(User.class, medico);
    }

    // ─── Cita (relaciones JPA) ───────────────────────────────────────

    @Test
    @DisplayName("Cita tiene relaciones @ManyToOne a Paciente y Medico")
    void cita_hasJpaRelationships() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setName("Paciente Test");

        Medico medico = new Medico();
        medico.setId(2L);
        medico.setName("Dr. Test");

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setEstado("Programada");
        cita.setTipo("Presencial");

        assertNotNull(cita.getPaciente(), "La cita debe tener un paciente asignado");
        assertNotNull(cita.getMedico(), "La cita debe tener un médico asignado");
        assertEquals("Paciente Test", cita.getPaciente().getName());
        assertEquals("Dr. Test", cita.getMedico().getName());
    }

    // ─── Informe (relaciones JPA) ────────────────────────────────────

    @Test
    @DisplayName("Informe tiene relaciones @ManyToOne a Paciente y Medico")
    void informe_hasJpaRelationships() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);

        Medico medico = new Medico();
        medico.setId(2L);

        Informe informe = new Informe();
        informe.setPaciente(paciente);
        informe.setMedico(medico);
        informe.setTitulo("Análisis de sangre");
        informe.setTipo("Laboratorio");
        informe.setFavorito(true);

        assertNotNull(informe.getPaciente());
        assertNotNull(informe.getMedico());
        assertEquals("Análisis de sangre", informe.getTitulo());
        assertTrue(informe.isFavorito());
    }

    // ─── Hospital (campos renombrados) ───────────────────────────────

    @Test
    @DisplayName("Hospital usa campos en español")
    void hospital_usesSpanishFieldNames() {
        Hospital hospital = new Hospital();
        hospital.setNombre("VitSync Centro Médico");
        hospital.setDireccion("Calle Principal 123");
        hospital.setTelefono("+34 912 345 678");
        hospital.setImagen("https://example.com/foto.jpg");

        assertEquals("VitSync Centro Médico", hospital.getNombre());
        assertEquals("Calle Principal 123", hospital.getDireccion());
        assertEquals("+34 912 345 678", hospital.getTelefono());
    }

    // ─── Especialidad ────────────────────────────────────────────────

    @Test
    @DisplayName("Especialidad: slug y código son accesibles")
    void especialidad_slugAndCodeWork() {
        Especialidad esp = new Especialidad();
        esp.setNombre("Traumatología");
        esp.setCodigo("TRAUMA");
        esp.setSlug("traumatologia");
        esp.setTipo("Quirúrgica");
        esp.setActivo(true);

        assertEquals("TRAUMA", esp.getCodigo());
        assertEquals("traumatologia", esp.getSlug());
        assertTrue(esp.getActivo());
    }
}
