package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.service.CitaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

import com.ejemplo.vitsync.dto.CitaRequest;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.service.EmailService;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;

/**
 * Controlador REST para la gestión de citas médicas.
 *
 * Endpoints:
 * - GET /api/citas → Lista todas las citas
 * - POST /api/citas → Crea una nueva cita y envía email de confirmación
 *
 * NOTA: @CrossOrigin eliminado porque CORS se gestiona globalmente en SecurityConfig.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CitaController.class);

    private final CitaService citaService;
    private final EmailService emailService;

    public CitaController(CitaService citaService, EmailService emailService) {
        this.citaService = citaService;
        this.emailService = emailService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<Cita>> getMisCitas() {
        String nif = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(citaService.getCitasByNif(nif));
    }

    @GetMapping
    public ResponseEntity<List<Cita>> getCitas() {
        return ResponseEntity.ok(citaService.getAllCitas());
    }

    /**
     * Cancela una cita del paciente autenticado (verifica propiedad para evitar
     * que un usuario cancele citas de otro — IDOR).
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelarCita(@PathVariable Long id) {
        Cita cita = citaService.getCitaById(id);
        if (cita == null) {
            return ResponseEntity.notFound().build();
        }
        String nif = SecurityContextHolder.getContext().getAuthentication().getName();
        if (cita.getPaciente() == null || cita.getPaciente().getNif() == null
                || !cita.getPaciente().getNif().equals(nif)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "No puedes cancelar esta cita"));
        }
        citaService.cancelCita(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Cita cancelada"));
    }

    @PostMapping
    public ResponseEntity<?> crearCita(@Valid @RequestBody CitaRequest request) {
        try {
            Cita cita = new Cita();

            // Asignar médico mediante referencia JPA (en vez de ID plano)
            if (request.getDoctor() != null && request.getDoctor().get("id") != null) {
                Object docId = request.getDoctor().get("id");
                if (!"any".equals(docId.toString())) {
                    try {
                        Medico medicoRef = new Medico();
                        medicoRef.setId(Long.parseLong(docId.toString()));
                        cita.setMedico(medicoRef);
                    } catch (NumberFormatException e) {
                        logger.warn("ID de médico no numérico: {}", docId);
                        return ResponseEntity.badRequest().body(java.util.Map.of("error", "ID de médico inválido"));
                    }
                }
            }

            // Parse fecha y hora ("2026-05-10T00:00:00.000Z" y "09:30")
            if (request.getDate() != null && request.getTime() != null) {
                try {
                    String dateStr = request.getDate().length() >= 10
                            ? request.getDate().substring(0, 10)
                            : request.getDate();
                    LocalDateTime fh = LocalDateTime.parse(dateStr + "T" + request.getTime() + ":00");
                    cita.setFechaHora(fh);
                } catch (java.time.format.DateTimeParseException | StringIndexOutOfBoundsException e) {
                    logger.warn("Formato de fecha/hora inválido: {}", e.getMessage());
                    return ResponseEntity.badRequest().body(java.util.Map.of("error", "Formato de fecha u hora inválido"));
                }
            }

            cita.setEstado("PROGRAMADA");
            cita.setTipo(request.getSpecialty() != null ? request.getSpecialty() : "General");

            // Asociar la cita al paciente autenticado: sin esto no aparece en
            // "Mis citas" (GET /api/citas/me filtra por el NIF del paciente).
            String nif = SecurityContextHolder.getContext().getAuthentication().getName();
            Paciente paciente = citaService.findPacienteByNif(nif);
            if (paciente != null) {
                cita.setPaciente(paciente);
            }

            Cita savedCita = citaService.saveCita(cita);

            // Email de confirmación (best-effort) al correo REAL del paciente
            try {
                if (paciente != null && paciente.getEmail() != null) {
                    String docName = request.getDoctor() != null
                            ? String.valueOf(request.getDoctor().getOrDefault("name", "Cualquier Profesional"))
                            : "Cualquier Profesional";
                    String hospitalName = request.getLocation() != null
                            ? String.valueOf(request.getLocation().getOrDefault("name", "VitSync Centro Médico"))
                            : "VitSync Centro Médico";
                    String fecha = request.getDate() != null && request.getDate().length() >= 10
                            ? request.getDate().substring(0, 10) : "";
                    String nombre = paciente.getName() != null ? paciente.getName() : "Paciente";
                    emailService.sendCitaConfirmationEmail(paciente.getEmail(), nombre, docName, fecha, request.getTime(), hospitalName);
                }
            } catch (Exception emailEx) {
                logger.warn("No se pudo enviar email de confirmación: {}", emailEx.getMessage());
            }

            return ResponseEntity.ok(savedCita);
        } catch (IllegalArgumentException ex) {
            logger.warn("Datos de cita inválidos: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Datos de la cita inválidos"));
        }
    }
}
