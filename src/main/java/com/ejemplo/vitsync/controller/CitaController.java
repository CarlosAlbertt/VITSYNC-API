package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.CitaResponse;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.MedicoRepository;
import com.ejemplo.vitsync.service.CitaService;
import com.ejemplo.vitsync.service.IUserService;
import com.ejemplo.vitsync.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;
    private final SecurityUtil securityUtil;
    private final IUserService userService;
    private final MedicoRepository medicoRepository;

    public CitaController(CitaService citaService, SecurityUtil securityUtil,
                          IUserService userService, MedicoRepository medicoRepository) {
        this.citaService = citaService;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.medicoRepository = medicoRepository;
    }

    /**
     * GET /api/citas
     * Devuelve las citas del usuario autenticado como CitaResponse (campos compatibles con el front).
     * - ADMIN: todas las citas.
     * - MEDICO: citas del médico.
     * - PACIENTE: citas del paciente.
     */
    @GetMapping
    public ResponseEntity<List<CitaResponse>> getCitas() {
        User currentUser = securityUtil.getCurrentUser();
        List<Cita> citas;
        if (currentUser.getRole() == Role.ADMIN) {
            citas = citaService.getAllCitas();
        } else if (currentUser.getRole() == Role.MEDICO) {
            citas = citaService.getCitasByMedicoId(currentUser.getId());
        } else {
            citas = citaService.getCitasByPacienteId(currentUser.getId());
        }
        List<CitaResponse> responses = citas.stream()
                .map(c -> enrichCita(c))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/citas/me
     * Igual que GET /api/citas pero explícitamente para el usuario autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<List<CitaResponse>> getMyCitas() {
        User currentUser = securityUtil.getCurrentUser();
        List<Cita> citas = (currentUser.getRole() == Role.MEDICO)
                ? citaService.getCitasByMedicoId(currentUser.getId())
                : citaService.getCitasByPacienteId(currentUser.getId());
        List<CitaResponse> responses = citas.stream()
                .map(c -> enrichCita(c))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/citas
     * Crea una nueva cita. Solo ADMIN o MEDICO pueden crearlas directamente.
     * Body esperado: { pacienteId, medicoId, fechaHora (ISO), tipo, estado, enlaceVideoconsulta }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponse> createCita(@RequestBody Map<String, Object> body) {
        Cita cita = new Cita();
        cita.setPacienteId(body.get("pacienteId") != null ? Long.valueOf(body.get("pacienteId").toString()) : null);
        cita.setMedicoId(body.get("medicoId") != null ? Long.valueOf(body.get("medicoId").toString()) : null);
        cita.setTipo(body.getOrDefault("tipo", "Presencial").toString());
        cita.setEstado(body.getOrDefault("estado", "PROGRAMADA").toString());
        cita.setEnlaceVideoconsulta(body.get("enlaceVideoconsulta") != null ? body.get("enlaceVideoconsulta").toString() : null);
        if (body.get("fechaHora") != null) {
            cita.setFechaHora(LocalDateTime.parse(body.get("fechaHora").toString()));
        }
        Cita saved = citaService.saveCita(cita);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrichCita(saved));
    }

    /**
     * PUT /api/citas/{id}/cancel
     * Cancela una cita si el usuario autenticado es el dueño o ADMIN.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelCita(@PathVariable Long id) {
        User currentUser = securityUtil.getCurrentUser();
        Cita cita = citaService.getCitaById(id);
        if (cita == null)
            return ResponseEntity.notFound().build();

        Long uid = currentUser.getId();
        boolean isOwner = (cita.getPacienteId() != null && cita.getPacienteId().equals(uid))
                || (cita.getMedicoId() != null && cita.getMedicoId().equals(uid));
        if (currentUser.getRole() != Role.ADMIN && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para cancelar esta cita");
        }

        citaService.cancelCita(id);
        return ResponseEntity.ok("Cita cancelada");
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    /** Enriquece una Cita con datos del médico y su especialidad para construir CitaResponse. */
    private CitaResponse enrichCita(Cita cita) {
        User medicoUser = null;
        String especialidadNombre = null;

        if (cita.getMedicoId() != null) {
            // Intentar obtener el Medico (con especialidad) para enriquecer la respuesta
            Optional<Medico> medicoOpt = medicoRepository.findByIdWithEspecialidad(cita.getMedicoId());
            if (medicoOpt.isPresent()) {
                Medico medico = medicoOpt.get();
                medicoUser = medico;
                if (medico.getEspecialidad() != null) {
                    especialidadNombre = medico.getEspecialidad().getNombre();
                }
            } else {
                // Fallback: buscar como User genérico
                medicoUser = userService.findById(cita.getMedicoId());
            }
        }
        return CitaResponse.fromEntity(cita, medicoUser, especialidadNombre);
    }
}
