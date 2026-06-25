package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.EnfermedadRequest;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.Enfermedad;
import com.ejemplo.vitsync.repository.EnfermedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de negocio del catálogo de enfermedades y tratamientos.
 */
@Service
@Transactional(readOnly = true)
public class EnfermedadService {

    private final EnfermedadRepository enfermedadRepository;

    public EnfermedadService(EnfermedadRepository enfermedadRepository) {
        this.enfermedadRepository = enfermedadRepository;
    }

    /** Catálogo público: solo enfermedades activas. */
    public List<Enfermedad> findAllActive() {
        return enfermedadRepository.findByActivoTrue();
    }

    /** Catálogo completo (admin): incluye inactivas. */
    public List<Enfermedad> findAll() {
        return enfermedadRepository.findAll();
    }

    public Enfermedad findById(Long id) {
        return enfermedadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enfermedad no encontrada con id " + id));
    }

    @Transactional
    public Enfermedad create(EnfermedadRequest request) {
        Enfermedad enfermedad = new Enfermedad();
        applyRequest(enfermedad, request);
        enfermedad.setActivo(request.getActivo() != null ? request.getActivo() : true);
        return enfermedadRepository.save(enfermedad);
    }

    @Transactional
    public Enfermedad update(Long id, EnfermedadRequest request) {
        Enfermedad enfermedad = findById(id);
        applyRequest(enfermedad, request);
        if (request.getActivo() != null) {
            enfermedad.setActivo(request.getActivo());
        }
        return enfermedadRepository.save(enfermedad);
    }

    @Transactional
    public void delete(Long id) {
        Enfermedad enfermedad = findById(id);
        enfermedadRepository.delete(enfermedad);
    }

    @Transactional
    public Enfermedad toggleActivo(Long id) {
        Enfermedad enfermedad = findById(id);
        enfermedad.setActivo(!Boolean.TRUE.equals(enfermedad.getActivo()));
        return enfermedadRepository.save(enfermedad);
    }

    private void applyRequest(Enfermedad enfermedad, EnfermedadRequest request) {
        enfermedad.setNombre(request.getNombre().trim());
        enfermedad.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        enfermedad.setEspecialidadRelacionada(request.getEspecialidadRelacionada().trim());
        // Copia defensiva de la lista de tratamientos (evita compartir la del DTO)
        enfermedad.setTratamientos(request.getTratamientos() != null
                ? new ArrayList<>(request.getTratamientos())
                : new ArrayList<>());
    }
}
