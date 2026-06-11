package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.Auditable;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.repository.CitaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CitaService {
    private final CitaRepository citaRepository;

    public CitaService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    public List<Cita> getAllCitas() {
        return citaRepository.findAll();
    }

    public Cita getCitaById(Long id) {
        return citaRepository.findById(id).orElse(null);
    }

    @Auditable(action = AuditAction.CANCEL_APPOINTMENT, targetIdIndex = 0)
    public void cancelCita(Long id) {
        Cita cita = getCitaById(id);
        if (cita != null) {
            cita.setEstado("Cancelada");
            citaRepository.save(cita);
        }
    }

    @Auditable(action = AuditAction.CREATE_APPOINTMENT)
    public Cita saveCita(Cita cita) {
        return citaRepository.save(cita);
    }

    public List<Cita> getCitasByNif(String nif) {
        return citaRepository.findByPaciente_Nif(nif);
    }
}
