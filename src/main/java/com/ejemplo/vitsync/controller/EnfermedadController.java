package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.EnfermedadRequest;
import com.ejemplo.vitsync.model.Enfermedad;
import com.ejemplo.vitsync.service.EnfermedadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Catálogo de enfermedades y tratamientos.
 *
 * <p>Lectura pública (catálogo activo); el CRUD y el listado completo
 * (incluye inactivas) están restringidos a ADMIN en {@code SecurityConfig}.
 * Errores y validación delegados a {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/enfermedades")
public class EnfermedadController {

    private final EnfermedadService enfermedadService;

    public EnfermedadController(EnfermedadService enfermedadService) {
        this.enfermedadService = enfermedadService;
    }

    // ─── Lectura pública ──────────────────────────────────────────────

    /** Lista las enfermedades activas (catálogo público). */
    @GetMapping
    public ResponseEntity<List<Enfermedad>> getActivas() {
        return ResponseEntity.ok(enfermedadService.findAllActive());
    }

    /** Lista TODAS las enfermedades, incluidas inactivas (ADMIN). */
    @GetMapping("/admin")
    public ResponseEntity<List<Enfermedad>> getTodas() {
        return ResponseEntity.ok(enfermedadService.findAll());
    }

    /** Detalle de una enfermedad por id. */
    @GetMapping("/{id}")
    public ResponseEntity<Enfermedad> getById(@PathVariable Long id) {
        return ResponseEntity.ok(enfermedadService.findById(id));
    }

    // ─── Escritura (ADMIN) ────────────────────────────────────────────

    /** Crea una enfermedad. */
    @PostMapping
    public ResponseEntity<Enfermedad> create(@Valid @RequestBody EnfermedadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enfermedadService.create(request));
    }

    /** Actualiza una enfermedad existente. */
    @PutMapping("/{id}")
    public ResponseEntity<Enfermedad> update(@PathVariable Long id,
                                             @Valid @RequestBody EnfermedadRequest request) {
        return ResponseEntity.ok(enfermedadService.update(id, request));
    }

    /** Elimina una enfermedad. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        enfermedadService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Enfermedad eliminada"));
    }

    /** Alterna el estado activo/inactivo. */
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<Enfermedad> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(enfermedadService.toggleActivo(id));
    }
}
