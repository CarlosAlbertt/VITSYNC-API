package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.MedicoRequest;
import com.ejemplo.vitsync.dto.MedicoResponse;
import com.ejemplo.vitsync.service.MedicoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private static final Logger logger = LoggerFactory.getLogger(MedicoController.class);

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    // ==================== ENDPOINTS GET (PÚBLICOS) ====================

    // GET /api/medicos – Listar todos los médicos activos
    @GetMapping
    public ResponseEntity<List<MedicoResponse>> getAllMedicos() {
        logger.info("Obteniendo todos los médicos activos");
        List<MedicoResponse> medicos = medicoService.findAllActive()
                .stream()
                .map(MedicoResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicos);
    }

    // GET /api/medicos/admin – Listar TODOS los médicos (activos e inactivos)
    @GetMapping("/admin")
    public ResponseEntity<List<MedicoResponse>> getAllMedicosAdmin() {
        logger.info("Obteniendo todos los médicos (admin, incluye inactivos)");
        List<MedicoResponse> medicos = medicoService.findAll()
                .stream()
                .map(MedicoResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicos);
    }

    // GET /api/medicos/{id} – Obtener médico por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicoById(@PathVariable Long id) {
        logger.info("Buscando médico con ID: {}", id);
        return medicoService.findById(id)
                .map(m -> ResponseEntity.ok(MedicoResponse.fromEntity(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/medicos/especialidad/{especialidadId} – Médicos de una especialidad
    @GetMapping("/especialidad/{especialidadId}")
    public ResponseEntity<List<MedicoResponse>> getMedicosByEspecialidad(@PathVariable Long especialidadId) {
        logger.info("Buscando médicos de especialidad ID: {}", especialidadId);
        List<MedicoResponse> medicos = medicoService.findByEspecialidad(especialidadId)
                .stream()
                .map(MedicoResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicos);
    }

    // ==================== ENDPOINTS DE ESCRITURA (ADMIN CRUD) ====================

    // POST /api/medicos – Crear nuevo médico
    @PostMapping
    public ResponseEntity<?> createMedico(@Valid @RequestBody MedicoRequest request) {
        logger.info("Creando nuevo médico: {} {}", request.getName(), request.getFirstName());
        try {
            MedicoResponse response = MedicoResponse.fromEntity(medicoService.create(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al crear médico: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/medicos/{id} – Actualizar médico existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedico(@PathVariable Long id,
                                           @Valid @RequestBody MedicoRequest request) {
        logger.info("Actualizando médico con ID: {}", id);
        try {
            MedicoResponse response = MedicoResponse.fromEntity(medicoService.update(id, request));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al actualizar médico {}: {}", id, e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    // DELETE /api/medicos/{id} – Eliminar médico
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedico(@PathVariable Long id) {
        logger.info("Eliminando médico con ID: {}", id);
        try {
            medicoService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error al eliminar médico {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH /api/medicos/{id}/toggle-activo – Activar o desactivar médico
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        logger.info("Alternando estado activo del médico con ID: {}", id);
        try {
            MedicoResponse response = MedicoResponse.fromEntity(medicoService.toggleActivo(id));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al alternar estado del médico {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
