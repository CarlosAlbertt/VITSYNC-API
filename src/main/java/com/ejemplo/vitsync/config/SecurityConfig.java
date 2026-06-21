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

                // Cabeceras de seguridad (V14): HSTS, anti-sniffing, anti-clickjacking.
                // CSP restrictiva porque la API solo sirve JSON (no HTML propio).
                .headers(headers -> headers
                        .contentTypeOptions(opts -> {})
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter(
                                "Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")))

                // Devolver 401/403 limpios sin redirigir a /login (API stateless)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN)))

                // Configurar autorización de peticiones.
                // IMPORTANTE: las reglas se evalúan EN ORDEN; las más específicas
                // van primero (V12: antes /api/medicos/** tapaba a /admin).
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Salud para el balanceador de Render
                        .requestMatchers("/actuator/health").permitAll()
                        // Autenticación pública (login, registro, verificación, refresh).
                        // logout-all exige token: se protege con anyRequest()
                        .requestMatchers("/api/auth/login", "/api/auth/login/2fa", "/api/auth/register",
                                "/api/auth/verify", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/validate").permitAll()
                        // Catálogo público para reserva de citas (solo lectura)
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/hospitales", "/api/hospitales/**",
                                "/api/horarios", "/api/horarios/**").permitAll()

                        // ===== MÉDICOS ===== (admin primero, luego lectura pública)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/medicos/admin").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/medicos", "/api/medicos/**").permitAll()
                        .requestMatchers("/api/medicos/**").hasRole("ADMIN")

                        // ===== ESPECIALIDADES =====
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/especialidades/admin").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/especialidades/**").authenticated()
                        .requestMatchers("/api/especialidades/**").hasRole("ADMIN")

                        // ===== CITAS ===== (crear cita exige autenticación: V11,
                        // evita spam de citas y envío de emails sin identificar)
                        .requestMatchers("/api/citas/**").authenticated()

                        // ===== ADMIN USUARIOS =====
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        // ===== PERFIL DE USUARIO =====
                        .requestMatchers("/VitSync-app/**").authenticated()
                        // ===== INFORMES / RELACIONES (datos clínicos) =====
                        .requestMatchers("/api/informes/**").authenticated()
                        .requestMatchers("/api/relationships/**").authenticated()
                        // ===== FICHEROS ===== (V09: ya no son públicos; contienen
                        // documentos médicos. La subida y la lectura exigen sesión)
                        .requestMatchers("/api/upload/**").authenticated()
                        .requestMatchers("/uploads/**").authenticated()
                        // WebSocket: el handshake pasa, la auth real es el
                        // interceptor STOMP en CONNECT (WebSocketAuthInterceptor)
                        .requestMatchers("/ws/**").permitAll()
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

        // Orígenes desde configuración de entorno (CORS_ALLOWED_ORIGINS)
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .forEach(allowedOrigins::add);
        }

        // Fallback de producción: solo los dominios oficiales. Los orígenes
        // localhost NO se incluyen aquí (V20: con allowCredentials abrirían la
        // API a páginas locales en producción). Para desarrollo, definir
        // CORS_ALLOWED_ORIGINS con los localhost en application-dev.properties.
        if (allowedOrigins.isEmpty()) {
            allowedOrigins.add("https://vitsync.es");
            allowedOrigins.add("https://www.vitsync.es");
        }

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
