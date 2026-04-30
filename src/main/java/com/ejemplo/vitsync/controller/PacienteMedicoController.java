package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.service.PacienteMedicoService;
import com.ejemplo.vitsync.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relationships")
public class PacienteMedicoController {

    private final PacienteMedicoService service;
    private final SecurityUtil securityUtil;

    public PacienteMedicoController(PacienteMedicoService service, SecurityUtil securityUtil) {
        this.service = service;
        this.securityUtil = securityUtil;
    }

    /** Solo ADMIN y MEDICO pueden asignar pacientes. */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MEDICO')")
    public ResponseEntity<String> assignPatientToProfessional(
            @RequestParam Long patientId,
            @RequestParam Long medicoId) {
        try {
            service.asignarMedicoAPaciente(patientId, medicoId);
            return ResponseEntity.ok("Asignado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** El paciente solo puede ver sus propios médicos; ADMIN puede ver cualquiera. */
    @GetMapping("/paciente/{id}/medicos")
    public ResponseEntity<List<Medico>> getMedicosDePaciente(@PathVariable Long id) {
        securityUtil.assertOwnerOrAdmin(id);
        return ResponseEntity.ok(service.getMedicosDePaciente(id));
    }

    /** El médico solo puede ver sus propios pacientes; ADMIN puede ver cualquiera. */
    @GetMapping("/medico/{id}/pacientes")
    public ResponseEntity<List<Paciente>> getPacientesDeMedico(@PathVariable Long id) {
        securityUtil.assertOwnerOrAdmin(id);
        return ResponseEntity.ok(service.getPacientesDeMedico(id));
    }
}
