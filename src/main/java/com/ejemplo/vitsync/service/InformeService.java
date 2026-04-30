package com.ejemplo.vitsync.service;

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

    public List<Informe> getAllInformes() {
        return informeRepository.findAll();
    }

    public List<Informe> getInformesByPacienteId(Long pacienteId) {
        return informeRepository.findByPacienteId(pacienteId);
    }

    public List<Informe> getInformesByMedicoId(Long medicoId) {
        return informeRepository.findByMedicoId(medicoId);
    }

    public Informe getInformeById(Long id) {
        return informeRepository.findById(id).orElse(null);
    }

    public Informe saveInforme(Informe informe) {
        return informeRepository.save(informe);
    }

    public void updateNotasPersonales(Long id, String notas) {
        Informe informe = getInformeById(id);
        if (informe != null) {
            informe.setNotasPersonales(notas);
            informeRepository.save(informe);
        }
    }
}
