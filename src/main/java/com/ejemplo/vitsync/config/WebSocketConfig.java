package com.ejemplo.vitsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * Clase de configuración para WebSocket con STOMP.
 * Define cómo se conectan los clientes y cómo fluyen los mensajes.
 *
 * @Configuration: Indica que es una clase de configuración de Spring.
 * @EnableWebSocketMessageBroker: Habilita el manejo de mensajes WebSocket
 *                                respaldado por un broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /** Orígenes permitidos para el handshake WebSocket (mismos que CORS HTTP). */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    /**
     * Paso 1: Registrar el endpoint de conexión.
     * Aquí es donde el frontend inicia el "handshake" (apretón de manos) inicial.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Restringimos los orígenes en vez de "*": el chat transporta datos
        // clínicos y allowCredentials con "*" sería inseguro (V08/V20)
        String[] origins = (allowedOrigins == null || allowedOrigins.isBlank())
                ? new String[]{"https://vitsync.es", "https://www.vitsync.es"}
                : Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                // Habilita SockJS como fallback si el navegador no soporta WebSocket nativo
                .withSockJS();
    }

    /**
     * Paso 0: Autenticar cada conexión STOMP entrante con el interceptor JWT,
     * de modo que el chat deje de ser anónimo (V08).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    /**
     * Paso 2: Configurar el broker de mensajes.
     * Define las "rutas" para enviar y recibir mensajes.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefijo para los mensajes que van DESDE el cliente HACIA el servidor.
        // Si el cliente envía a "/app/chat", llegará al @MessageMapping("/chat") del
        // controlador.
        registry.setApplicationDestinationPrefixes("/app");

        // Habilita un broker de memoria simple para enviar mensajes DESDE el servidor
        // HACIA el cliente.
        // Los clientes se suscribirán a rutas que empiecen por "/user" o "/queue".
        registry.enableSimpleBroker(
                "/user",
                "/queue");

        // Prefijo específico para enviar mensajes privados a un usuario concreto.
        // Spring gestiona internamente la traducción de "/user/{username}/queue/..." a
        // la sesión del usuario.
        registry.setUserDestinationPrefix("/user");
    }
}
