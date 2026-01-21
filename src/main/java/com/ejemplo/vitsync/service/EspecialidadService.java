package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Especialidad;
import com.ejemplo.vitsync.repository.EspecialidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadService(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    // Obtener todas las especialidades activas con sus médicos
    public List<Especialidad> findAllActive() {
        return especialidadRepository.findAllActiveWithMedicos();
    }

    // Obtener todas las especialidades
    public List<Especialidad> findAll() {
        return especialidadRepository.findAll();
    }

    // Buscar por ID (con médicos)
    public Optional<Especialidad> findById(Long id) {
        return especialidadRepository.findByIdWithMedicos(id);
    }

    // Buscar por slug (con médicos)
    public Optional<Especialidad> findBySlug(String slug) {
        return especialidadRepository.findBySlugWithMedicos(slug);
    }

    // Buscar por código
    public Optional<Especialidad> findByCodigo(String codigo) {
        return especialidadRepository.findByCodigo(codigo);
    }

    // Buscar por tipo
    public List<Especialidad> findByTipo(String tipo) {
        return especialidadRepository.findByTipoAndActivoTrue(tipo);
    }
}
