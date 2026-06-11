package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.config.SecurityUtils;
import com.ejemplo.vitsync.dto.ChatNotification;
import com.ejemplo.vitsync.model.Mensaje;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.service.ChatService;
import com.ejemplo.vitsync.util.HtmlSanitizer;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;

/**
 * Real-time chat controller (STOMP over WebSocket) plus the REST history
 * endpoint.
 *
 * <p>Security: the sender is derived from the authenticated STOMP principal,
 * never trusted from the payload (spoofing fix, V08); message text is
 * sanitised before persistence (stored XSS, V16); and history is readable
 * only by the two participants (IDOR, V03/V04).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final SecurityUtils securityUtils;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatService chatService,
                          SecurityUtils securityUtils) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.securityUtils = securityUtils;
    }

    /**
     * Receives a chat message, persists it and pushes it to the recipient.
     *
     * @param mensaje   message payload (its {@code senderId} is ignored and
     *                  overwritten with the authenticated sender)
     * @param principal authenticated STOMP user (NIF), set by the JWT
     *                  interceptor at CONNECT
     * @throws AccessDeniedException if the session is not authenticated
     */
    @MessageMapping("/chat")
    public void processMessage(@Payload Mensaje mensaje, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("WebSocket no autenticado");
        }
        // El emisor real lo determina el token, no el cliente (anti-spoofing)
        User sender = securityUtils.getCurrentUserByNif(principal.getName());
        mensaje.setSenderId(sender.getId());
        mensaje.setContent(HtmlSanitizer.sanitize(mensaje.getContent()));

        Mensaje saved = chatService.save(mensaje);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(saved.getRecipientId()),
                "/queue/messages",
                new ChatNotification(
                        saved.getId(),
                        saved.getSenderId(),
                        saved.getRecipientId(),
                        saved.getContent(),
                        saved.getTimestamp()));
    }

    /**
     * Returns the conversation between two users. The caller must be one of
     * the two participants (or an admin).
     *
     * @param senderId    one participant id
     * @param recipientId the other participant id
     * @return the ordered message history
     * @throws AccessDeniedException if the caller is not a participant
     */
    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<Mensaje>> findChatMessages(
            @PathVariable Long senderId,
            @PathVariable Long recipientId) {

        // El que consulta debe ser uno de los dos interlocutores
        if (!securityUtils.isAdmin()) {
            Long me = securityUtils.getCurrentUser().getId();
            if (!me.equals(senderId) && !me.equals(recipientId)) {
                throw new AccessDeniedException("Acceso denegado");
            }
        }
        return ResponseEntity.ok(chatService.findChatHistory(senderId, recipientId));
    }
}
