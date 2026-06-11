package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.service.InformeService;
import com.ejemplo.vitsync.util.HtmlSanitizer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Medical report endpoints.
 *
 * <p>Access is strictly scoped: a patient sees only their own reports
 * ({@code /me}); the unrestricted listing is ADMIN/MEDICO only (it used to
 * return every report to any authenticated user — audit finding V04). Note
 * edits verify ownership (IDOR, V03) and sanitise the input (stored XSS,
 * V16).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    /**
     * Returns the authenticated patient's own reports.
     *
     * @param authentication injected principal (NIF)
     * @return reports owned by the caller
     */
    @GetMapping("/me")
    public ResponseEntity<List<Informe>> getMisInformes(Authentication authentication) {
        return ResponseEntity.ok(informeService.getInformesByNif(authentication.getName()));
    }

    /**
     * Lists all reports. Restricted to ADMIN and MEDICO roles.
     *
     * @return all reports
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<Informe>> getInformes() {
        return ResponseEntity.ok(informeService.getAllInformes());
    }

    /**
     * Updates the personal notes of one of the caller's own reports.
     *
     * @param id             report id
     * @param payload        {@code {"notasPersonales": "..."}}
     * @param authentication injected principal (NIF)
     * @return 200 on success; 403 if the report is not the caller's; 404 if
     *         the report does not exist
     */
    @PutMapping("/{id}/notes")
    public ResponseEntity<String> updateNotes(@PathVariable Long id,
                                              @RequestBody Map<String, String> payload,
                                              Authentication authentication) {
        // Sanitizar el texto libre antes de persistir (defensa XSS)
        String notas = HtmlSanitizer.sanitize(payload.getOrDefault("notasPersonales", ""));
        informeService.updateNotasPersonales(id, notas, authentication.getName());
        return ResponseEntity.ok("Notas actualizadas");
    }
}
