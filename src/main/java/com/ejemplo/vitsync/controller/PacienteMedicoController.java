package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.service.PacienteMedicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relationships")
public class PacienteMedicoController {

    private final PacienteMedicoService service;

    public PacienteMedicoController(PacienteMedicoService service) {
        this.service = service;
    }

    @PostMapping("/assign")
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

    @GetMapping("/paciente/{id}/medicos")
    public ResponseEntity<List<Medico>> getMedicosDePaciente(@PathVariable Long id) {
        return ResponseEntity.ok(service.getMedicosDePaciente(id));
    }

    @GetMapping("/medico/{id}/pacientes")
    public ResponseEntity<List<Paciente>> getPacientesDeMedico(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPacientesDeMedico(id));
    }
}
