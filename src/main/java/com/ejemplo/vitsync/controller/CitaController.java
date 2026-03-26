package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.service.CitaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @GetMapping
    public ResponseEntity<List<Cita>> getCitas() {
        return ResponseEntity.ok(citaService.getAllCitas());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelCita(@PathVariable Long id) {
        citaService.cancelCita(id);
        return ResponseEntity.ok("Cita cancelada");
    }
}
