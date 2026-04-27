package com.ejemplo.vitsync.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que protege los endpoints públicos (permitAll) exigiendo
 * un header {@code X-API-Key} válido en cada petición.
 * <p>
 * Esto impide el acceso directo desde la barra de direcciones del
 * navegador, ya que el navegador no envía headers personalizados
 * al navegar a una URL.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${app.api-key}")
    private String expectedApiKey;

    /**
     * Rutas públicas que requieren la API Key para ser accedidas.
     * Las rutas de auth, websocket, uploads y error NO la requieren.
     */
    private static final List<String> PROTECTED_PUBLIC_PATHS = List.of(
            "/api/especialidades",
            "/api/medicos"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // No interferir con preflight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();

        // Solo aplicar a las rutas públicas protegidas
        boolean requiresApiKey = PROTECTED_PUBLIC_PATHS.stream()
                .anyMatch(requestPath::startsWith);

        if (requiresApiKey) {
            // Si el usuario ya está autenticado con JWT, dejar pasar
            // (los admins y usuarios logueados no necesitan API Key)
            if (request.getHeader("Authorization") != null
                    && request.getHeader("Authorization").startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validar la API Key
            String providedApiKey = request.getHeader(API_KEY_HEADER);

            if (providedApiKey == null || !providedApiKey.equals(expectedApiKey)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"error\": \"Acceso denegado. Se requiere una API Key válida.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
