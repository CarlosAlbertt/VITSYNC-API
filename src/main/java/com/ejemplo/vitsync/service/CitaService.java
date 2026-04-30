package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<Cita> getCitasByPacienteId(Long pacienteId) {
        return citaRepository.findByPacienteId(pacienteId);
    }

    public List<Cita> getCitasByMedicoId(Long medicoId) {
        return citaRepository.findByMedicoId(medicoId);
    }

    public Cita getCitaById(Long id) {
        return citaRepository.findById(id).orElse(null);
    }

    public Cita saveCita(Cita cita) {
        return citaRepository.save(cita);
    }

    @Transactional
    public void cancelCita(Long id) {
        Cita cita = getCitaById(id);
        if (cita != null) {
            cita.setEstado("CANCELADA");
            citaRepository.save(cita);
        }
    }
}
