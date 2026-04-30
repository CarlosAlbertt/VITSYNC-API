package com.ejemplo.vitsync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF para APIs REST
                .csrf(AbstractHttpConfigurer::disable)

                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Permitir frames same-origin (necesario para la consola H2 en dev)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // Configurar autorización de peticiones
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Consola H2 (solo activa con perfil dev)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Autenticación (registro, login, verificación)
                        .requestMatchers("/api/auth/**").permitAll()
                        // ===== ESPECIALIDADES =====
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/especialidades/admin").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/especialidades/**").permitAll()
                        .requestMatchers("/api/especialidades/**").hasRole("ADMIN")

                        // ===== ENFERMEDADES =====
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/enfermedades/admin").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/enfermedades/**").permitAll()
                        .requestMatchers("/api/enfermedades/**").hasRole("ADMIN")

                        // ===== MÉDICOS =====
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/medicos/admin").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/medicos/**").permitAll()
                        .requestMatchers("/api/medicos/**").hasRole("ADMIN")

                        // ===== ADMIN USUARIOS =====
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        // ===== USER PROFILE =====
                        .requestMatchers("/VitSync-app/**").authenticated()
                        // ===== RELATIONSHIPS (CHAT CONTACTS) =====
                        .requestMatchers("/api/relationships/**").authenticated()
                        // WebSocket
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Permitir leer ficheros (fotos, docs)
                        // Página de error de Spring
                        .requestMatchers("/error").permitAll()
                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated())

                // Sesiones stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Filtro JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins = new ArrayList<>();

        // Añadir orígenes desde configuración
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .forEach(allowedOrigins::add);
        }

        // Añadir orígenes crìticos explícitamente (Fallback para producción)
        allowedOrigins.add("http://localhost:5173");
        allowedOrigins.add("http://localhost:3000");
        // Fallback para producción
        allowedOrigins.add("http://localhost:4000"); // Puerto en el que estás ejecutando Vite
        allowedOrigins.add("https://vitsync.es");
        allowedOrigins.add("https://www.vitsync.es");

        configuration.setAllowedOrigins(allowedOrigins.stream().distinct().toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
