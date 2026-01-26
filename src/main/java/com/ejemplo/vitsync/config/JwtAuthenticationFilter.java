package com.ejemplo.vitsync.config;

import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

    // 🔥 Permitir preflight CORS
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        filterChain.doFilter(request, response);
        return;
    }

    // 🔑 Leer header Authorization
    String authHeader = request.getHeader("Authorization");

    // Si no hay token o no es Bearer, continuar sin autenticar
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        // Extraer el token (quitar "Bearer ")
        final String jwt = authHeader.substring(7);

        // Extraer el nif del token
        final String nif = jwtUtil.extractNif(jwt);

        // Si hay nif y no hay autenticación previa
        if (nif != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Verificar que el usuario existe
            userRepository.findByNif(nif).ifPresent(user -> {

                // Validar el token
                if (jwtUtil.validateToken(jwt, nif)) {

                    // Extraer el rol del token
                    String role = jwtUtil.extractRole(jwt);

                    // Crear autenticación
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    nif,
                                    null,
                                    Collections.singletonList(
                                            new SimpleGrantedAuthority("ROLE_" + role)
                                    )
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Establecer autenticación
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            });
        }

    } catch (Exception e) {
        logger.error("Error procesando JWT", e);
    }

    filterChain.doFilter(request, response);
}

}
