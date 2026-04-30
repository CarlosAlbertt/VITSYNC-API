package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.EnfermedadRequest;
import com.ejemplo.vitsync.model.Enfermedad;
import com.ejemplo.vitsync.repository.EnfermedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EnfermedadService {

    private final EnfermedadRepository enfermedadRepository;

    public EnfermedadService(EnfermedadRepository enfermedadRepository) {
        this.enfermedadRepository = enfermedadRepository;
    }

    // ==================== LECTURA ====================

    public List<Enfermedad> findAllActive() {
        return enfermedadRepository.findAllActiveWithTratamientos();
    }

    public List<Enfermedad> findAll() {
        return enfermedadRepository.findAllWithTratamientos();
    }

    public Optional<Enfermedad> findById(Long id) {
        return enfermedadRepository.findById(id);
    }

    // ==================== ESCRITURA ====================

    @Transactional
    public Enfermedad create(EnfermedadRequest request) {
        if (enfermedadRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe una enfermedad con el nombre: " + request.getNombre());
        }

        Enfermedad enfermedad = new Enfermedad();
        enfermedad.setNombre(request.getNombre());
        enfermedad.setDescripcion(request.getDescripcion());
        enfermedad.setTratamientos(request.getTratamientos());
        enfermedad.setEspecialidadRelacionada(request.getEspecialidadRelacionada());
        enfermedad.setActivo(request.getActivo() != null ? request.getActivo() : true);

        return enfermedadRepository.save(enfermedad);
    }

    @Transactional
    public Enfermedad update(Long id, EnfermedadRequest request) {
        Enfermedad enfermedad = enfermedadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enfermedad no encontrada con ID: " + id));

        if (enfermedadRepository.existsByNombreAndIdNot(request.getNombre(), id)) {
            throw new IllegalArgumentException("Ya existe otra enfermedad con el nombre: " + request.getNombre());
        }

        enfermedad.setNombre(request.getNombre());
        enfermedad.setDescripcion(request.getDescripcion());
        enfermedad.setTratamientos(request.getTratamientos());
        enfermedad.setEspecialidadRelacionada(request.getEspecialidadRelacionada());
        if (request.getActivo() != null) {
            enfermedad.setActivo(request.getActivo());
        }

        return enfermedadRepository.save(enfermedad);
    }

    @Transactional
    public void delete(Long id) {
        Enfermedad enfermedad = enfermedadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enfermedad no encontrada con ID: " + id));
        enfermedadRepository.delete(enfermedad);
    }

    @Transactional
    public Enfermedad toggleActivo(Long id) {
        Enfermedad enfermedad = enfermedadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enfermedad no encontrada con ID: " + id));
        enfermedad.setActivo(!enfermedad.getActivo());
        return enfermedadRepository.save(enfermedad);
    }
}
