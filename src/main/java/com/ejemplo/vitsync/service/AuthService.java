package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Random;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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

        if (!user.isVerified()) {
            throw new RuntimeException("Cuenta no verificada. Por favor revisa tu correo.");
        }
        // Generar token JWT
        String token = jwtUtil.generateToken(user.getNif(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
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

        // Crear nuevo usuario basado en el rol
        User user;
        if (request.getRole() == com.ejemplo.vitsync.enums.Role.PACIENTE) {
            user = new com.ejemplo.vitsync.model.Paciente();
        } else if (request.getRole() == Role.MEDICO) {
            user = new com.ejemplo.vitsync.model.Medico();
            // TODO: Si hay campos específicos de médico en el registro, setearlos aquí
        } else {
            user = new User(); // Fallback o Admin
        }

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

        String randomCode = String.valueOf(new Random().nextInt(899999) + 100000);
        user.setVerificationCode(randomCode);
        user.setVerified(false);
        // Guardar usuario
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), randomCode);

        // Generar token JWT
        String token = jwtUtil.generateToken(user.getNif(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .nif(user.getNif())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Usuario registrado exitosamente")
                .build();
    }

    public void verifyAccount(String email, String code) {
        userRepository.findByEmail(email);

        if (userRepository.existsByEmail(email)) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (user.getVerificationCode().equals(code)) {
                user.setVerified(true);
                user.setVerificationCode(null);
                userRepository.save(user);

                emailService.sendWelcomeEmail(user.getEmail());
            } else {
                throw new RuntimeException("Código de verificación incorrecto");
            }
        } else {
            throw new RuntimeException("El email no está registrado");
        }
    }
}
