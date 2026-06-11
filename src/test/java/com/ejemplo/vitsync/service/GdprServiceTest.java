package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.AuditService;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.AuditLogRepository;
import com.ejemplo.vitsync.repository.CitaRepository;
import com.ejemplo.vitsync.repository.InformeRepository;
import com.ejemplo.vitsync.repository.MensajeRepository;
import com.ejemplo.vitsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GdprService}: data access (Art. 15), portability
 * export (Art. 20) and erasure via anonymisation (Art. 17).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GdprService")
class GdprServiceTest {

    @Mock UserRepository userRepository;
    @Mock CitaRepository citaRepository;
    @Mock InformeRepository informeRepository;
    @Mock MensajeRepository mensajeRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock RefreshTokenService refreshTokenService;
    @Mock EmailService emailService;
    @Mock AuditService auditService;

    @InjectMocks GdprService gdprService;

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(7L);
        paciente.setNif("12345678Z");
        paciente.setName("Ana");
        paciente.setEmail("ana@test.com");
        paciente.setGrupoSanguineo("A+");
        paciente.setAlergias("polen");
    }

    private void stubUserFound(User user) {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    private void stubEmptyCollections(String nif, Long id) {
        when(citaRepository.findByPaciente_Nif(nif)).thenReturn(new ArrayList<>());
        lenient().when(informeRepository.findByPaciente_Nif(nif)).thenReturn(List.of());
        lenient().when(mensajeRepository.findBySenderIdOrRecipientId(id, id)).thenReturn(List.of());
    }

    // ---- collectUserData (Art. 15) ----

    @Test
    @DisplayName("collectUserData: usuario inexistente → ResourceNotFoundException")
    void collectUserData_notFound_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> gdprService.collectUserData(9L));
    }

    @Test
    @DisplayName("collectUserData: paciente → incluye perfil con datos clínicos descifrados")
    void collectUserData_paciente_includesClinicalData() {
        stubUserFound(paciente);
        stubEmptyCollections("12345678Z", 7L);

        Map<String, Object> data = gdprService.collectUserData(7L);

        assertNotNull(data.get("exportedAt"));
        @SuppressWarnings("unchecked")
        Map<String, Object> perfil = (Map<String, Object>) data.get("perfil");
        assertEquals("12345678Z", perfil.get("nif"));
        assertEquals("ana@test.com", perfil.get("email"));
        @SuppressWarnings("unchecked")
        Map<String, Object> clinical = (Map<String, Object>) perfil.get("datosClinicos");
        assertEquals("A+", clinical.get("grupoSanguineo"));
        assertEquals("polen", clinical.get("alergias"));
        assertTrue(data.containsKey("citas"));
        assertTrue(data.containsKey("informes"));
        assertTrue(data.containsKey("mensajes"));
    }

    @Test
    @DisplayName("collectUserData: usuario no paciente → sin bloque datosClinicos")
    void collectUserData_plainUser_noClinicalBlock() {
        User user = new User();
        user.setId(7L);
        user.setNif("00000000T");
        stubUserFound(user);
        stubEmptyCollections("00000000T", 7L);

        Map<String, Object> data = gdprService.collectUserData(7L);

        @SuppressWarnings("unchecked")
        Map<String, Object> perfil = (Map<String, Object>) data.get("perfil");
        assertFalse(perfil.containsKey("datosClinicos"));
    }

    // ---- exportAsZip (Art. 20) ----

    @Test
    @DisplayName("exportAsZip: genera ZIP con datos.json y datos.txt + auditoría")
    void exportAsZip_buildsZipWithBothEntries() throws Exception {
        stubUserFound(paciente);
        stubEmptyCollections("12345678Z", 7L);

        byte[] zipBytes = gdprService.exportAsZip(7L);

        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        assertEquals(List.of("datos.json", "datos.txt"), entries);
        verify(auditService).record(eq(AuditAction.EXPORT_DATA), eq("7"), eq(true), isNull());
    }

    @Test
    @DisplayName("exportAsZip: usuario inexistente → ResourceNotFoundException sin auditoría")
    void exportAsZip_notFound_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> gdprService.exportAsZip(9L));
        verify(auditService, never()).record(any(), anyString(), anyBoolean(), any());
    }

    // ---- requestDeletion (Art. 17, fase 1) ----

    @Test
    @DisplayName("requestDeletion: suspende cuenta, revoca sesiones y agenda anonimización a 30 días")
    void requestDeletion_suspendsAndSchedules() {
        stubUserFound(paciente);
        when(citaRepository.findByPaciente_Nif("12345678Z")).thenReturn(List.of());

        LocalDate scheduled = gdprService.requestDeletion(7L);

        assertEquals(LocalDate.now().plusDays(30), scheduled);
        assertTrue(paciente.getSuspended());
        verify(userRepository).save(paciente);
        verify(refreshTokenService).revokeAll(paciente);
        verify(emailService).sendDeletionRequestEmail("ana@test.com", scheduled.toString());
        verify(auditService).record(eq(AuditAction.DATA_DELETION_REQUEST), eq("7"), eq(true),
                eq("scheduled=" + scheduled));
    }

    @Test
    @DisplayName("requestDeletion: cancela solo citas futuras no canceladas y avisa al médico")
    void requestDeletion_cancelsOnlyFutureActiveCitas() {
        stubUserFound(paciente);

        Medico medico = new Medico();
        medico.setEmail("dr@test.com");

        Cita futura = new Cita();
        futura.setFechaHora(LocalDateTime.now().plusDays(5));
        futura.setEstado("Programada");
        futura.setMedico(medico);

        Cita pasada = new Cita();
        pasada.setFechaHora(LocalDateTime.now().minusDays(5));
        pasada.setEstado("Completada");

        Cita yaCancelada = new Cita();
        yaCancelada.setFechaHora(LocalDateTime.now().plusDays(3));
        yaCancelada.setEstado("Cancelada");

        when(citaRepository.findByPaciente_Nif("12345678Z"))
                .thenReturn(List.of(futura, pasada, yaCancelada));

        gdprService.requestDeletion(7L);

        assertEquals("Cancelada", futura.getEstado());
        assertEquals("Completada", pasada.getEstado());
        verify(citaRepository, times(1)).save(any(Cita.class));
        verify(emailService).sendAppointmentCancelledByErasureEmail(
                eq("dr@test.com"), eq(futura.getFechaHora().toLocalDate().toString()));
    }

    @Test
    @DisplayName("requestDeletion: cita futura sin médico → cancela sin email al médico")
    void requestDeletion_futureCitaWithoutMedico_noClinicianEmail() {
        stubUserFound(paciente);

        Cita futura = new Cita();
        futura.setFechaHora(LocalDateTime.now().plusDays(2));
        futura.setEstado("Programada");

        when(citaRepository.findByPaciente_Nif("12345678Z")).thenReturn(List.of(futura));

        gdprService.requestDeletion(7L);

        assertEquals("Cancelada", futura.getEstado());
        verify(emailService, never()).sendAppointmentCancelledByErasureEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("requestDeletion: usuario inexistente → ResourceNotFoundException")
    void requestDeletion_notFound_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> gdprService.requestDeletion(9L));
        verify(refreshTokenService, never()).revokeAll(any());
    }

    // ---- anonymizeUser (Art. 17, fase 2) ----

    @Test
    @DisplayName("anonymizeUser: sustituye identificadores por pseudónimo irreversible")
    void anonymizeUser_replacesIdentifiers() {
        stubUserFound(paciente);

        gdprService.anonymizeUser(7L);

        assertEquals("ANONIMIZADO", paciente.getName());
        assertTrue(paciente.getNif().startsWith("ANON-"));
        assertEquals(21, paciente.getNif().length()); // "ANON-" + 16 hex
        assertTrue(paciente.getEmail().endsWith("@anonimizado.invalid"));
        assertEquals("000000000", paciente.getPhone());
        assertTrue(paciente.getSuspended());
        assertNull(paciente.getVerificationCode());
        verify(userRepository).save(paciente);
    }

    @Test
    @DisplayName("anonymizeUser: paciente → limpia texto clínico libre")
    void anonymizeUser_clearsClinicalFreeText() {
        paciente.setCondicionesPrevias("asma");
        paciente.setContactoEmergencia("Luis 600000000");
        stubUserFound(paciente);

        gdprService.anonymizeUser(7L);

        assertNull(paciente.getAlergias());
        assertNull(paciente.getCondicionesPrevias());
        assertNull(paciente.getContactoEmergencia());
        assertNull(paciente.getGrupoSanguineo());
        assertNull(paciente.getHistorialClinicoId());
    }

    @Test
    @DisplayName("anonymizeUser: re-asocia el rastro de auditoría al pseudónimo")
    void anonymizeUser_pseudonymizesAuditTrail() {
        stubUserFound(paciente);

        gdprService.anonymizeUser(7L);

        verify(auditLogRepository).pseudonymizeActor(eq("12345678Z"), startsWith("ANON-"));
        verify(auditService).record(eq(AuditAction.DATA_DELETION_REQUEST),
                startsWith("ANON-"), eq(true), eq("anonymized"));
    }

    @Test
    @DisplayName("anonymizeUser: mismo usuario → mismo pseudónimo (determinista)")
    void anonymizeUser_pseudonymIsDeterministic() {
        stubUserFound(paciente);
        gdprService.anonymizeUser(7L);
        String first = paciente.getNif();

        Paciente otra = new Paciente();
        otra.setId(7L);
        otra.setNif("12345678Z");
        when(userRepository.findById(7L)).thenReturn(Optional.of(otra));
        gdprService.anonymizeUser(7L);

        assertEquals(first, otra.getNif());
    }

    @Test
    @DisplayName("anonymizeUser: usuario inexistente → ResourceNotFoundException")
    void anonymizeUser_notFound_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> gdprService.anonymizeUser(9L));
        verify(auditLogRepository, never()).pseudonymizeActor(anyString(), anyString());
    }
}
