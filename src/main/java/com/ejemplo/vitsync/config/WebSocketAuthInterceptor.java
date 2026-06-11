package com.ejemplo.vitsync.config;

import com.ejemplo.vitsync.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * STOMP channel interceptor that authenticates the WebSocket session at
 * CONNECT time using the JWT supplied in the STOMP {@code Authorization}
 * header.
 *
 * <p>Without this, the chat WebSocket was fully anonymous: a client could
 * spoof {@code senderId} and subscribe to other users' private queues (audit
 * finding V08). Once authenticated, the principal NIF is bound to the STOMP
 * session and {@code ChatController} derives the real sender from it instead
 * of trusting the payload.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Solo validamos en el CONNECT inicial: el principal persiste en la sesión
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "WebSocket sin token");
            }
            String token = authHeader.substring(7);
            String nif = jwtUtil.extractNif(token);
            if (nif == null || !jwtUtil.validateToken(token, nif)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Token WebSocket inválido");
            }

            String role = jwtUtil.extractRole(token);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            nif, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            accessor.setUser(auth);
        }
        return message;
    }
}
