package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.Auditable;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.repository.CitaRepository;
import com.ejemplo.vitsync.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CitaService {
    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;

    public CitaService(CitaRepository citaRepository, PacienteRepository pacienteRepository) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
    }

    /** Patient matching the NIF, or null. Used to associate a new cita to its owner. */
    public Paciente findPacienteByNif(String nif) {
        return pacienteRepository.findByNif(nif).orElse(null);
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

    /** ¿Hay ya una cita activa para ese médico a esa hora exacta? (control de concurrencia). */
    public boolean isSlotTaken(Long medicoId, java.time.LocalDateTime fechaHora) {
        return citaRepository.existsActiveSlot(medicoId, fechaHora);
    }
}
