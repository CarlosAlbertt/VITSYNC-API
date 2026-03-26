package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.MedicoRequest;
import com.ejemplo.vitsync.model.Especialidad;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.repository.EspecialidadRepository;
import com.ejemplo.vitsync.repository.MedicoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final PasswordEncoder passwordEncoder;

    public MedicoService(MedicoRepository medicoRepository,
                         EspecialidadRepository especialidadRepository,
                         PasswordEncoder passwordEncoder) {
        this.medicoRepository = medicoRepository;
        this.especialidadRepository = especialidadRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== LECTURA ====================

    // Obtener todos los médicos activos (con especialidad)
    public List<Medico> findAllActive() {
        return medicoRepository.findAllActiveWithEspecialidad();
    }

    // Obtener todos los médicos incluyendo inactivos (admin)
    public List<Medico> findAll() {
        return medicoRepository.findAllWithEspecialidad();
    }

    // Buscar por ID
    public Optional<Medico> findById(Long id) {
        return medicoRepository.findByIdWithEspecialidad(id);
    }

    // Buscar por número de colegiado
    public Optional<Medico> findByNumeroColegiado(String numeroColegiado) {
        return medicoRepository.findByNumeroColegiado(numeroColegiado);
    }

    // Buscar médicos por especialidad
    public List<Medico> findByEspecialidad(Long especialidadId) {
        return medicoRepository.findByEspecialidadId(especialidadId);
    }

    // ==================== ESCRITURA ====================

    // Crear nuevo médico
    @Transactional
    public Medico create(MedicoRequest request) {
        // Validar unicidad de número de colegiado
        if (medicoRepository.existsByNumeroColegiado(request.getNumeroColegiado())) {
            throw new IllegalArgumentException("Ya existe un médico con el número de colegiado: " + request.getNumeroColegiado());
        }

        // Validar que la contraseña se proporciona en creación
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria al crear un médico");
        }

        Medico medico = new Medico();
        mapRequestToMedico(request, medico);
        medico.setPassword(passwordEncoder.encode(request.getPassword()));
        medico.setRole(Role.MEDICO);

        return medicoRepository.save(medico);
    }

    // Actualizar médico existente
    @Transactional
    public Medico update(Long id, MedicoRequest request) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado con ID: " + id));

        // Validar unicidad de número de colegiado (excluyendo el actual)
        if (medicoRepository.existsByNumeroColegiadoAndIdNot(request.getNumeroColegiado(), id)) {
            throw new IllegalArgumentException("Ya existe otro médico con el número de colegiado: " + request.getNumeroColegiado());
        }

        mapRequestToMedico(request, medico);

        // Actualizar contraseña solo si se proporciona
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            medico.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return medicoRepository.save(medico);
    }

    // Eliminar médico
    @Transactional
    public void delete(Long id) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado con ID: " + id));
        medicoRepository.delete(medico);
    }

    // Activar / desactivar médico
    @Transactional
    public Medico toggleActivo(Long id) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado con ID: " + id));
        medico.setActivo(!medico.getActivo());
        return medicoRepository.save(medico);
    }

    // ==================== UTILIDADES PRIVADAS ====================

    private void mapRequestToMedico(MedicoRequest request, Medico medico) {
        medico.setName(request.getName());
        medico.setFirstName(request.getFirstName());
        medico.setSecondName(request.getSecondName());
        medico.setNif(request.getNif());
        medico.setEmail(request.getEmail());
        medico.setGender(request.getGender());
        medico.setBirthDate(request.getBirthDate());
        medico.setPhone(request.getPhone());
        medico.setAddress(request.getAddress());
        medico.setPostCode(request.getPostCode());
        medico.setCountry(request.getCountry());
        medico.setNumeroColegiado(request.getNumeroColegiado());
        medico.setFotoUrl(request.getFotoUrl());
        medico.setBio(request.getBio());
        medico.setActivo(request.getActivo() != null ? request.getActivo() : true);

        // Asignar especialidad si se proporciona el ID
        if (request.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                    .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + request.getEspecialidadId()));
            medico.setEspecialidad(especialidad);
        } else {
            medico.setEspecialidad(null);
        }
    }
}
