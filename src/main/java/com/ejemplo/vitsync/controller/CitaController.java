package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.service.CitaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.ejemplo.vitsync.dto.CitaRequest;
import com.ejemplo.vitsync.service.EmailService;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
public class CitaController {

    private final CitaService citaService;
    private final EmailService emailService;

    public CitaController(CitaService citaService, EmailService emailService) {
        this.citaService = citaService;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<List<Cita>> getCitas() {
        return ResponseEntity.ok(citaService.getAllCitas());
    }

    @PostMapping
    public ResponseEntity<?> crearCita(@RequestBody CitaRequest request) {
        try {
            Cita cita = new Cita();
            
            // Parse doctor ID
            if (request.getDoctor() != null && request.getDoctor().get("id") != null) {
                Object docId = request.getDoctor().get("id");
                if (!docId.toString().equals("any")) {
                    cita.setMedicoId(Long.parseLong(docId.toString()));
                }
            }
            
            // Parse fecha y hora ("2026-05-10T00:00:00.000Z" y "09:30")
            if (request.getDate() != null && request.getTime() != null) {
                String dateStr = request.getDate().substring(0, 10);
                LocalDateTime fh = LocalDateTime.parse(dateStr + "T" + request.getTime() + ":00");
                cita.setFechaHora(fh);
            }
            
            cita.setEstado("PROGRAMADA");
            cita.setTipo(request.getSpecialty() != null ? request.getSpecialty() : "General");
            
            Cita savedCita = citaService.saveCita(cita);
            
            // Enviar email simulado (idealmente sacado del contexto de seguridad)
            String pacienteNombre = "Paciente";
            String emailDestino = "paciente@ejemplo.com"; // En un caso real: usuarioService.getLogueado().getEmail()
            String docName = request.getDoctor() != null ? (String) request.getDoctor().get("name") : "Cualquier Profesional";
            String hospitalName = request.getLocation() != null ? (String) request.getLocation().get("name") : "VitSync Centro Médico";
            String fecha = request.getDate() != null ? request.getDate().substring(0, 10) : "";
            
            emailService.sendCitaConfirmationEmail(emailDestino, pacienteNombre, docName, fecha, request.getTime(), hospitalName);
            
            return ResponseEntity.ok(savedCita);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al guardar cita: " + ex.getMessage());
        }
    }
}
