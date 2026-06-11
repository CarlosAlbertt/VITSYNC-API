package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== LECTURA ====================

    /**
     * Returns a page of all users.
     *
     * @param pageable page request
     * @return page of users
     */
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Returns a page of users filtered by role, filtering in the database
     * (the previous version loaded the whole table and filtered in memory —
     * audit finding V19).
     *
     * @param role     role to filter by
     * @param pageable page request
     * @return page of users with that role
     */
    public Page<User> findByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    // Obtener un usuario por ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Obtener un usuario por email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ==================== ESCRITURA ====================

    // Actualizar datos de usuario existente (sin cambiar contraseña obligatoriamente)
    @Transactional
    public User update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Validar unicidad de email (excluyendo el usuario actual) → 409
        userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new DataIntegrityViolationException("Ya existe otro usuario con ese email");
                });

        // Validar unicidad de NIF (excluyendo el usuario actual) → 409
        userRepository.findByNif(request.getNif())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new DataIntegrityViolationException("Ya existe otro usuario con ese NIF");
                });

        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setSecondName(request.getSecondName());
        user.setNif(request.getNif());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setBirthDate(request.getBirthDate());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setPostCode(request.getPostCode());
        user.setCountry(request.getCountry());

        // Actualizar contraseña solo si se proporciona
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    // Eliminar usuario
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        userRepository.delete(user);
    }

    // Verificar manualmente un usuario (marcar como verificado sin email)
    @Transactional
    public User setVerified(Long id, boolean verified) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        user.setVerified(verified);
        return userRepository.save(user);
    }
}
