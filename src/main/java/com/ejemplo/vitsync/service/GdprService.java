package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.AuditService;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.*;
import com.ejemplo.vitsync.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implements the GDPR data-subject rights: access &amp; portability
 * (Arts. 15/20) and erasure (Art. 17).
 *
 * <p>Erasure is performed as <b>anonymisation</b>, not physical deletion:
 * clinical documentation must be retained for legal periods (Ley 41/2002
 * art. 17), and audit logs are append-only legal evidence. The person is made
 * unidentifiable (identifiers replaced by a one-way pseudonym) while the
 * decoupled records remain. The full procedure is in
 * {@code docs/GDPR_PROCEDURES.md}.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Service
public class GdprService {

    private final UserRepository userRepository;
    private final CitaRepository citaRepository;
    private final InformeRepository informeRepository;
    private final MensajeRepository mensajeRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /** Statutory waiting period before erasure is executed. */
    private static final int DELETION_WAITING_DAYS = 30;

    public GdprService(UserRepository userRepository, CitaRepository citaRepository,
                       InformeRepository informeRepository, MensajeRepository mensajeRepository,
                       AuditLogRepository auditLogRepository, RefreshTokenService refreshTokenService,
                       EmailService emailService, AuditService auditService) {
        this.userRepository = userRepository;
        this.citaRepository = citaRepository;
        this.informeRepository = informeRepository;
        this.mensajeRepository = mensajeRepository;
        this.auditLogRepository = auditLogRepository;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Collects everything the system holds about a user (Art. 15 access).
     *
     * @param userId user id
     * @return structured map: profile, appointments, reports, messages
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public Map<String, Object> collectUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("nif", user.getNif());
        profile.put("name", user.getName());
        profile.put("firstName", user.getFirstName());
        profile.put("secondName", user.getSecondName());
        profile.put("email", user.getEmail());
        profile.put("gender", user.getGender());
        profile.put("role", user.getRole());
        profile.put("birthDate", user.getBirthDate());
        profile.put("phone", user.getPhone());
        profile.put("address", user.getAddress());
        profile.put("postCode", user.getPostCode());
        profile.put("country", user.getCountry());

        // Los campos clínicos cifrados se devuelven descifrados (los lee el converter)
        if (user instanceof Paciente paciente) {
            Map<String, Object> clinical = new LinkedHashMap<>();
            clinical.put("grupoSanguineo", paciente.getGrupoSanguineo());
            clinical.put("alergias", paciente.getAlergias());
            clinical.put("condicionesPrevias", paciente.getCondicionesPrevias());
            clinical.put("contactoEmergencia", paciente.getContactoEmergencia());
            profile.put("datosClinicos", clinical);
        }

        List<Cita> citas = citaRepository.findByPaciente_Nif(user.getNif());
        List<Informe> informes = informeRepository.findByPaciente_Nif(user.getNif());
        List<Mensaje> mensajes = mensajeRepository.findBySenderIdOrRecipientId(user.getId(), user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exportedAt", LocalDateTime.now());
        result.put("perfil", profile);
        result.put("citas", citas);
        result.put("informes", informes);
        result.put("mensajes", mensajes);
        return result;
    }

    /**
     * Produces a ZIP with the user's data for portability (Art. 20):
     * {@code datos.json} (machine-readable) plus {@code datos.txt}
     * (human-readable summary).
     *
     * @param userId user id
     * @return ZIP archive bytes
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public byte[] exportAsZip(Long userId) {
        Map<String, Object> data = collectUserData(userId);
        try {
            byte[] json = objectMapper.writeValueAsBytes(data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(baos)) {
                zip.putNextEntry(new ZipEntry("datos.json"));
                zip.write(json);
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("datos.txt"));
                zip.write(toReadableText(data).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            auditService.record(AuditAction.EXPORT_DATA, String.valueOf(userId), true, null);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la exportación de datos", e);
        }
    }

    /**
     * Initiates erasure (Art. 17): suspends the account, revokes all sessions,
     * cancels future appointments (notifying the clinician) and emails the
     * user with the scheduled anonymisation date. Actual anonymisation runs
     * after the {@value #DELETION_WAITING_DAYS}-day waiting period via
     * {@link #anonymizeUser(Long)}.
     *
     * @param userId user id
     * @return the scheduled anonymisation date
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public LocalDate requestDeletion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setSuspended(true);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);

        // Cancelar citas futuras y avisar a los médicos implicados
        LocalDateTime now = LocalDateTime.now();
        for (Cita cita : citaRepository.findByPaciente_Nif(user.getNif())) {
            if (cita.getFechaHora() != null && cita.getFechaHora().isAfter(now)
                    && !"Cancelada".equalsIgnoreCase(cita.getEstado())) {
                cita.setEstado("Cancelada");
                citaRepository.save(cita);
                if (cita.getMedico() != null && cita.getMedico().getEmail() != null) {
                    emailService.sendAppointmentCancelledByErasureEmail(
                            cita.getMedico().getEmail(),
                            cita.getFechaHora().toLocalDate().toString());
                }
            }
        }

        LocalDate scheduled = LocalDate.now().plusDays(DELETION_WAITING_DAYS);
        emailService.sendDeletionRequestEmail(user.getEmail(), scheduled.toString());
        auditService.record(AuditAction.DATA_DELETION_REQUEST, String.valueOf(userId), true,
                "scheduled=" + scheduled);
        return scheduled;
    }

    /**
     * Executes anonymisation: replaces direct identifiers with an irreversible
     * pseudonym, clears clinical free-text, and re-points audit rows to the
     * pseudonym so the legal trail survives but is decoupled from the person.
     *
     * <p>Intended to run after the waiting period (scheduled job or admin
     * action). Does NOT delete the row, preserving referential integrity for
     * retained clinical/appointment records.</p>
     *
     * @param userId user id
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public void anonymizeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String originalNif = user.getNif();
        String pseudonym = "ANON-" + sha256(originalNif + ":" + user.getId()).substring(0, 16);

        user.setName("ANONIMIZADO");
        user.setFirstName("ANONIMIZADO");
        user.setSecondName("ANONIMIZADO");
        user.setNif(pseudonym);
        user.setEmail(pseudonym + "@anonimizado.invalid");
        user.setPhone("000000000");
        user.setAddress("ANONIMIZADO");
        user.setPostCode("00000");
        user.setVerificationCode(null);
        user.setSuspended(true);

        // Borrar texto clínico libre (mantiene la fila por integridad referencial)
        if (user instanceof Paciente paciente) {
            paciente.setAlergias(null);
            paciente.setCondicionesPrevias(null);
            paciente.setContactoEmergencia(null);
            paciente.setGrupoSanguineo(null);
            paciente.setHistorialClinicoId(null);
        }
        userRepository.save(user);

        // Mantener el rastro de auditoría pero disociado de la identidad
        auditLogRepository.pseudonymizeActor(originalNif, pseudonym);
        auditService.record(AuditAction.DATA_DELETION_REQUEST, pseudonym, true, "anonymized");
    }

    private String toReadableText(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXPORTACIÓN DE DATOS PERSONALES — VitSync\n");
        sb.append("Generado: ").append(data.get("exportedAt")).append("\n\n");
        sb.append("Este archivo contiene todos los datos personales que VitSync\n");
        sb.append("trata sobre ti (RGPD Arts. 15 y 20). El detalle estructurado\n");
        sb.append("está en datos.json.\n\n");
        Object perfil = data.get("perfil");
        sb.append("PERFIL:\n").append(perfil).append("\n\n");
        sb.append("Nº de citas: ").append(((List<?>) data.get("citas")).size()).append("\n");
        sb.append("Nº de informes: ").append(((List<?>) data.get("informes")).size()).append("\n");
        sb.append("Nº de mensajes: ").append(((List<?>) data.get("mensajes")).size()).append("\n");
        return sb.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
