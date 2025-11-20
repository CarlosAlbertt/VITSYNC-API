package com.ejemplo.vitsync.config;

import com.ejemplo.vitsync.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
// ❌ Eliminado @RequiredArgsConstructor
// ❌ Eliminado @Lazy y las inyecciones de campo
public class SecurityConfig {

    /**
     * Configuración principal de seguridad.
     * Recibe todas las dependencias necesarias (filtro y proveedor) como parámetros de método.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            // ✅ Inyectamos el filtro aquí
            JwtAuthenticationFilter jwtAuthFilter,
            // ✅ Inyectamos el proveedor de autenticación aquí
            AuthenticationProvider authenticationProvider
    ) throws Exception {
        http
                // Deshabilitar CSRF (no es necesario para APIs REST con JWT)
                .csrf(csrf -> csrf.disable())

                // Configurar CORS
                .cors(cors -> cors.configure(http))

                .cors(withDefaults())

                // Configurar autorización de endpoints
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (no requieren autenticación)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()

                        // Todos los demás endpoints requieren autenticación
                        .anyRequest().authenticated()
                )

                // No crear sesiones (usamos JWT, sin estado)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Agregar nuestro filtro JWT antes del filtro de autenticación de Spring
                // ⚠️ Usamos el bean inyectado como parámetro
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Proveedor de autenticación.
     * Recibe AuthService (UserDetailsService) y PasswordEncoder como parámetros.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            AuthService authService,
            PasswordEncoder passwordEncoder // Spring resuelve los Beans por ti
    ) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Gestor de autenticación
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Codificador de contraseñas (BCrypt es el estándar)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8080")); // O de donde venga tu frontend
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}