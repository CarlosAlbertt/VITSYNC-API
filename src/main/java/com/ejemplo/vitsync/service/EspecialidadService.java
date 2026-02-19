package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.EspecialidadRequest;
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

    // ==================== LECTURA ====================

    // Obtener todas las especialidades activas con sus médicos
    public List<Especialidad> findAllActive() {
        return especialidadRepository.findAllActiveWithMedicos();
    }

    // Obtener todas las especialidades (incluidas inactivas, para admin)
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

    // ==================== ESCRITURA ====================

    // Crear nueva especialidad
    @Transactional
    public Especialidad create(EspecialidadRequest request) {
        // Validar unicidad de código
        if (especialidadRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una especialidad con el código: " + request.getCodigo());
        }

        // Generar slug si no se proporciona
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getNombre());
        }

        // Validar unicidad de slug
        if (especialidadRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Ya existe una especialidad con el slug: " + slug);
        }

        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(request.getNombre());
        especialidad.setCodigo(request.getCodigo());
        especialidad.setDescripcion(request.getDescripcion());
        especialidad.setTipo(request.getTipo().toUpperCase());
        especialidad.setSlug(slug);
        especialidad.setIcono(request.getIcono());
        especialidad.setActivo(request.getActivo() != null ? request.getActivo() : true);

        return especialidadRepository.save(especialidad);
    }

    // Actualizar especialidad existente
    @Transactional
    public Especialidad update(Long id, EspecialidadRequest request) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + id));

        // Validar unicidad de código (excluyendo la actual)
        if (especialidadRepository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
            throw new IllegalArgumentException("Ya existe otra especialidad con el código: " + request.getCodigo());
        }

        // Generar slug si no se proporciona
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getNombre());
        }

        // Validar unicidad de slug (excluyendo la actual)
        if (especialidadRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException("Ya existe otra especialidad con el slug: " + slug);
        }

        especialidad.setNombre(request.getNombre());
        especialidad.setCodigo(request.getCodigo());
        especialidad.setDescripcion(request.getDescripcion());
        especialidad.setTipo(request.getTipo().toUpperCase());
        especialidad.setSlug(slug);
        especialidad.setIcono(request.getIcono());
        if (request.getActivo() != null) {
            especialidad.setActivo(request.getActivo());
        }

        return especialidadRepository.save(especialidad);
    }

    // Eliminar especialidad
    @Transactional
    public void delete(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + id));
        especialidadRepository.delete(especialidad);
    }

    // Activar/desactivar especialidad
    @Transactional
    public Especialidad toggleActivo(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + id));
        especialidad.setActivo(!especialidad.getActivo());
        return especialidadRepository.save(especialidad);
    }

    // ==================== UTILIDADES ====================

    // Generar slug a partir del nombre
    private String generateSlug(String nombre) {
        return nombre.toLowerCase()
                .replaceAll("[áàäâã]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöôõ]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("ñ", "n")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
