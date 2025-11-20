package com.ejemplo.vitsync.config; // Asegúrate que el paquete sea correcto

import com.ejemplo.vitsync.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // <-- ¡¡IMPORTANTE!! Esto soluciona tu error original
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService; // Spring inyectará tu AuthService aquí

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Obtener el header "Authorization"
        final String authHeader = request.getHeader("Authorization");

        // 2. Comprobar si es nulo o no empieza con "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Pasa al siguiente filtro
            return;
        }

        // 3. Extraer el token (quitando "Bearer ")
        final String token = authHeader.substring(7);
        final String username;

        try {
            // 4. Extraer el username del token
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            // Si el token es inválido (expirado, malformado), no autenticamos
            filterChain.doFilter(request, response);
            return;
        }


        // 5. Comprobar que el usuario no esté ya autenticado
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Cargar los detalles del usuario desde la BBDD (usando tu AuthService)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 7. Validar el token contra los detalles del usuario
            if (jwtUtil.validateToken(token, userDetails)) {
                // 8. Crear el objeto de autenticación
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // No se necesitan credenciales (password)
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 9. Establecer la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. Pasar al siguiente filtro
        filterChain.doFilter(request, response);
    }
}