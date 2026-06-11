package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.Auditable;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.repository.InformeRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InformeService {
    private final InformeRepository informeRepository;

    public InformeService(InformeRepository informeRepository) {
        this.informeRepository = informeRepository;
    }

    @Auditable(action = AuditAction.VIEW_MEDICAL_REPORT)
    public List<Informe> getAllInformes() {
        return informeRepository.findAll();
    }

    public Informe getInformeById(Long id) {
        return informeRepository.findById(id).orElse(null);
    }

    /**
     * Updates the personal notes of a report, verifying ownership.
     *
     * @param id       report id
     * @param notas    sanitised note text
     * @param ownerNif NIF that must match the report's patient (IDOR check)
     * @throws com.ejemplo.vitsync.exception.ResourceNotFoundException if the
     *         report does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         report belongs to another patient
     */
    public void updateNotasPersonales(Long id, String notas, String ownerNif) {
        Informe informe = informeRepository.findById(id)
                .orElseThrow(() -> new com.ejemplo.vitsync.exception.ResourceNotFoundException(
                        "Informe no encontrado"));

        // El informe debe pertenecer al paciente autenticado
        if (informe.getPaciente() == null || !ownerNif.equals(informe.getPaciente().getNif())) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }

        informe.setNotasPersonales(notas);
        informeRepository.save(informe);
    }

    @Auditable(action = AuditAction.VIEW_MEDICAL_REPORT, targetIdIndex = 0)
    public List<Informe> getInformesByNif(String nif) {
        return informeRepository.findByPaciente_Nif(nif);
    }
}
