package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.service.InformeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<Informe>> getMisInformes() {
        String nif = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(informeService.getInformesByNif(nif));
    }

    @GetMapping
    public ResponseEntity<List<Informe>> getInformes() {
        return ResponseEntity.ok(informeService.getAllInformes());
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<String> updateNotes(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String notas = payload.getOrDefault("notasPersonales", "");
        informeService.updateNotasPersonales(id, notas);
        return ResponseEntity.ok("Notas actualizadas");
    }
}
