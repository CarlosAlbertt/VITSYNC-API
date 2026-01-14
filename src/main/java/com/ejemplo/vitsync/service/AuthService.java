package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // Login de usuario
    public AuthResponse login(LoginRequest request) {
        // Buscar usuario por username
        User user = userRepository.findByNif(request.getNif())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // Generar token JWT
        String token = jwtUtil.generateToken(user.getNif(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .nif(user.getNif())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Login exitoso")
                .build();
    }

    // Registro de nuevo usuario
    public AuthResponse register(RegisterRequest request) {
        // Verificar si el username ya existe
        if (userRepository.existsByNif(request.getNif())) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        // Verificar si el email ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setSecondName(request.getSecondName());
        user.setNif(request.getNif());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encriptar contraseña
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setBirthDate(request.getBirthDate());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setPostCode(request.getPostCode());
        user.setCountry(request.getCountry());

        // Guardar usuario
        userRepository.save(user);

        // Generar token JWT
        String token = jwtUtil.generateToken(user.getNif(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .nif(user.getNif())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Usuario registrado exitosamente")
                .build();
    }
}
