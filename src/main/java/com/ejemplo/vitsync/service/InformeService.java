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

    public Informe getInformeById(Long id) {
        return informeRepository.findById(id).orElse(null);
    }

    public void updateNotasPersonales(Long id, String notas) {
        Informe informe = getInformeById(id);
        if (informe != null) {
            informe.setNotasPersonales(notas);
            informeRepository.save(informe);
        }
    }
}
