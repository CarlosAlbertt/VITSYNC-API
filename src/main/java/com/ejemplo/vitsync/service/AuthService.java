package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;



    /**
     * LOGIN - Autentica al usuario y genera un token JWT
     */
    @Transactional
    public AuthResponse login(@Valid LoginRequest request) {

        // 2. Si llega aquí, las credenciales son correctas
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // 3. Generar token JWT
        String token = jwtUtil.generateToken(user);

        // 4. Construir y retornar la respuesta
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .role(String.valueOf(user.getRole()))
                .message("Login exitoso")
                .build();
    }

    /**
     * REGISTER - Registra un nuevo usuario
     */
    @Transactional
    public AuthResponse register(@Valid RegisterRequest request) {
        // 1. Validar que el username no exista
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El username ya está en uso");
        }

        // 2. Validar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // 3. Crear nuevo usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encriptar contraseña
        user.setNombre(request.getNombre());
        user.setApellidos(request.getApellidos());
        user.setActivo(true);

        // 4. Guardar en la base de datos
        User savedUser = userRepository.save(user);

        // 5. Generar token JWT
        String token = jwtUtil.generateToken(savedUser);

        // 6. Retornar respuesta
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .nombre(savedUser.getNombre())
                .role(String.valueOf(savedUser.getRole()))
                .message("Usuario registrado exitosamente")
                .build();
    }

    /**
     * Cargar usuario por username (requerido por Spring Security)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con username: " + username
                ));
    }
}