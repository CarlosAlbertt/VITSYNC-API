package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.ChatNotification;
import com.ejemplo.vitsync.model.Mensaje;
import com.ejemplo.vitsync.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat")
    public void processMessage(@Payload Mensaje mensaje) {
        Mensaje saved = chatService.save(mensaje);

        // Enviar al destinatario específico (Cola privada)
        messagingTemplate.convertAndSendToUser(
                String.valueOf(mensaje.getRecipientId()),
                "/queue/messages",
                new ChatNotification(
                        saved.getId(),
                        saved.getSenderId(),
                        saved.getRecipientId(),
                        saved.getContent(),
                        saved.getTimestamp()));
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<Mensaje>> findChatMessages(
            @PathVariable Long senderId,
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(chatService.findChatHistory(senderId, recipientId));
    }
}
