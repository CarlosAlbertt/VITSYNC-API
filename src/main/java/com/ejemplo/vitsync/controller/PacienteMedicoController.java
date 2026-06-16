package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.AssignRelationshipRequest;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.service.PacienteMedicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relationships")
public class PacienteMedicoController {

    private final PacienteMedicoService service;

    public PacienteMedicoController(PacienteMedicoService service) {
        this.service = service;
    }

    /**
     * Assigns a patient to a medical professional.
     *
     * Accepts a JSON body ({@link AssignRelationshipRequest}) instead of query
     * parameters so identifiers are not exposed in logs/history. Validation and
     * domain errors are handled centrally by GlobalExceptionHandler, so no
     * internal exception detail is leaked to the client.
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, String>> assignPatientToProfessional(
            @Valid @RequestBody AssignRelationshipRequest request) {
        service.asignarMedicoAPaciente(request.getPatientId(), request.getMedicoId());
        return ResponseEntity.ok(Map.of("message", "Asignado exitosamente"));
    }

    @GetMapping("/paciente/{id}/medicos")
    public ResponseEntity<List<Medico>> getMedicosDePaciente(@PathVariable Long id) {
        return ResponseEntity.ok(service.getMedicosDePaciente(id));
    }

    @GetMapping("/medico/{id}/pacientes")
    public ResponseEntity<List<Paciente>> getPacientesDeMedico(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPacientesDeMedico(id));
    }
}
