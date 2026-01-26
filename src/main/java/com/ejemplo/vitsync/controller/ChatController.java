package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.ChatMessage;
import com.ejemplo.vitsync.repository.ChatMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Controlador que maneja tanto Websockets como peticiones HTTP REST para el
 * chat.
 */
@Controller
public class ChatController {

    // Herramienta de Spring para enviar mensajes WebSocket desde el código Java
    SimpMessagingTemplate simpMessagingTemplate;

    // Repositorio para guardar/leer mensajes de la BD
    ChatMessageRepository chatMessageRepository;

    // Inyección de dependencias por constructor
    public ChatController(SimpMessagingTemplate simpMessagingTemplate, ChatMessageRepository chatMessageRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Recibe mensajes enviados por los usuarios vía WebSocket.
     * 
     * @MessageMapping("/chat"): Escucha mensajes enviados a la ruta "/app/chat"
     * (prefijo /app definido en config).
     * 
     * @Payload: El cuerpo del mensaje convertido automáticamente a objeto
     *           ChatMessage.
     */
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        // 1. Guardar el mensaje en la base de datos para tener historial persistente
        ChatMessage saved = chatMessageRepository.save(chatMessage);

        /**
         * 2. Reenviar el mensaje al destinatario específico.
         * 
         * convertAndSendToUser hace magia:
         * - Toma el ID del destinatario.
         * - Lo envía a la ruta "/queue/messages".
         * - Internamente, Spring lo dirige a la sesión WebSocket de ese usuario
         * concreto
         * (que estará escuchando en "/user/queue/messages").
         */
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getRecipientId()), // Usuario destino
                "/queue/messages", // Ruta destino
                saved); // Payload (mensaje)
    }

    /**
     * Endpoint REST simple para recuperar el historial de chat entre dos usuarios.
     * Se llama via HTTP (GET) al abrir la ventana de chat.
     */
    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessage>> findChatMessages(
            @PathVariable Long senderId,
            @PathVariable Long recipientId) {

        // Busca todos los mensajes intercambiados entre senderId y recipientId, en
        // ambas direcciones
        return ResponseEntity.ok(
                chatMessageRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                        senderId, recipientId, recipientId, senderId));
    }

}
