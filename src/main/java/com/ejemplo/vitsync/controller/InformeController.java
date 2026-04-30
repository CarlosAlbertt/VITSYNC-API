package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.InformeResponse;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.MedicoRepository;
import com.ejemplo.vitsync.service.IUserService;
import com.ejemplo.vitsync.service.InformePdfService;
import com.ejemplo.vitsync.service.InformeService;
import com.ejemplo.vitsync.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private static final Logger logger = LoggerFactory.getLogger(InformeController.class);

    private final InformeService informeService;
    private final InformePdfService informePdfService;
    private final IUserService userService;
    private final SecurityUtil securityUtil;
    private final MedicoRepository medicoRepository;

    public InformeController(InformeService informeService, InformePdfService informePdfService,
                             IUserService userService, SecurityUtil securityUtil,
                             MedicoRepository medicoRepository) {
        this.informeService = informeService;
        this.informePdfService = informePdfService;
        this.userService = userService;
        this.securityUtil = securityUtil;
        this.medicoRepository = medicoRepository;
    }

    /**
     * GET /api/informes
     * Devuelve los informes del usuario autenticado como InformeResponse (campos compatibles con el front).
     * - ADMIN: todos los informes.
     * - MEDICO: informes emitidos por el médico.
     * - PACIENTE: informes del paciente.
     */
    @GetMapping
    public ResponseEntity<List<InformeResponse>> getInformes() {
        User currentUser = securityUtil.getCurrentUser();
        List<Informe> informes;
        if (currentUser.getRole() == Role.ADMIN) {
            informes = informeService.getAllInformes();
        } else if (currentUser.getRole() == Role.MEDICO) {
            informes = informeService.getInformesByMedicoId(currentUser.getId());
        } else {
            informes = informeService.getInformesByPacienteId(currentUser.getId());
        }
        List<InformeResponse> responses = informes.stream()
                .map(i -> enrichInforme(i))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/informes/me
     * Igual que GET /api/informes pero explícitamente para el usuario autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<List<InformeResponse>> getMyInformes() {
        User currentUser = securityUtil.getCurrentUser();
        List<Informe> informes = (currentUser.getRole() == Role.MEDICO)
                ? informeService.getInformesByMedicoId(currentUser.getId())
                : informeService.getInformesByPacienteId(currentUser.getId());
        List<InformeResponse> responses = informes.stream()
                .map(i -> enrichInforme(i))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/informes
     * Crea un nuevo informe. Solo ADMIN o MEDICO pueden crearlos.
     * Body esperado: { pacienteId, medicoId, titulo, tipo, fecha (yyyy-MM-dd), archivoUrl, notasPersonales, favorito }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<InformeResponse> createInforme(@RequestBody Map<String, Object> body) {
        Informe informe = new Informe();
        informe.setPacienteId(body.get("pacienteId") != null ? Long.valueOf(body.get("pacienteId").toString()) : null);
        informe.setMedicoId(body.get("medicoId") != null ? Long.valueOf(body.get("medicoId").toString()) : null);
        informe.setTitulo(body.getOrDefault("titulo", "Informe médico").toString());
        informe.setTipo(body.getOrDefault("tipo", "Consulta").toString());
        informe.setArchivoUrl(body.get("archivoUrl") != null ? body.get("archivoUrl").toString() : null);
        informe.setNotasPersonales(body.get("notasPersonales") != null ? body.get("notasPersonales").toString() : null);
        informe.setFavorito(body.get("favorito") != null && Boolean.parseBoolean(body.get("favorito").toString()));
        if (body.get("fecha") != null) {
            informe.setFecha(LocalDate.parse(body.get("fecha").toString()));
        } else {
            informe.setFecha(LocalDate.now());
        }
        Informe saved = informeService.saveInforme(informe);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrichInforme(saved));
    }

    /**
     * GET /api/informes/{id}/pdf
     * Genera y descarga el PDF de un informe.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getInformePdf(@PathVariable Long id) {
        User currentUser = securityUtil.getCurrentUser();
        Informe informe = informeService.getInformeById(id);
        if (informe == null) return ResponseEntity.notFound().build();

        Long uid = currentUser.getId();
        boolean isOwner = (informe.getPacienteId() != null && informe.getPacienteId().equals(uid))
                || (informe.getMedicoId() != null && informe.getMedicoId().equals(uid));
        if (currentUser.getRole() != Role.ADMIN && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User paciente = informe.getPacienteId() != null ? userService.findById(informe.getPacienteId()) : null;
        User medico   = informe.getMedicoId()   != null ? userService.findById(informe.getMedicoId())   : null;

        try {
            byte[] pdf = informePdfService.generate(informe, paciente, medico);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "informe-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error generando PDF para informe {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/informes/{id}/notes
     * Actualiza las notas personales de un informe.
     */
    @PutMapping("/{id}/notes")
    public ResponseEntity<String> updateNotes(@PathVariable Long id,
                                              @RequestBody Map<String, String> payload) {
        User currentUser = securityUtil.getCurrentUser();
        Informe informe = informeService.getInformeById(id);
        if (informe == null) return ResponseEntity.notFound().build();

        Long uid = currentUser.getId();
        boolean isOwner = (informe.getPacienteId() != null && informe.getPacienteId().equals(uid))
                || (informe.getMedicoId() != null && informe.getMedicoId().equals(uid));
        if (currentUser.getRole() != Role.ADMIN && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para editar este informe");
        }

        String notas = payload.getOrDefault("notasPersonales", "");
        informeService.updateNotasPersonales(id, notas);
        return ResponseEntity.ok("Notas actualizadas");
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    /** Enriquece un Informe con datos del médico y su especialidad para construir InformeResponse. */
    private InformeResponse enrichInforme(Informe informe) {
        User medicoUser = null;
        String especialidadNombre = null;

        if (informe.getMedicoId() != null) {
            Optional<Medico> medicoOpt = medicoRepository.findByIdWithEspecialidad(informe.getMedicoId());
            if (medicoOpt.isPresent()) {
                Medico medico = medicoOpt.get();
                medicoUser = medico;
                if (medico.getEspecialidad() != null) {
                    especialidadNombre = medico.getEspecialidad().getNombre();
                }
            } else {
                medicoUser = userService.findById(informe.getMedicoId());
            }
        }
        return InformeResponse.fromEntity(informe, medicoUser, especialidadNombre);
    }
}
