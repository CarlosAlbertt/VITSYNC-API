package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    // Obtener todos los usuarios
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Obtener usuarios filtrados por rol
    public List<User> findByRole(Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .toList();
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
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        // Validar unicidad de email (excluyendo el usuario actual)
        userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe otro usuario con el email: " + request.getEmail());
                });

        // Validar unicidad de NIF (excluyendo el usuario actual)
        userRepository.findByNif(request.getNif())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe otro usuario con el NIF: " + request.getNif());
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
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
        userRepository.delete(user);
    }

    // Verificar manualmente un usuario (marcar como verificado sin email)
    @Transactional
    public User setVerified(Long id, boolean verified) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
        user.setVerified(verified);
        return userRepository.save(user);
    }
}
