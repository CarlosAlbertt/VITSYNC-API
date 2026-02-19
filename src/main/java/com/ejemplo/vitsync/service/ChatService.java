package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Mensaje;
import com.ejemplo.vitsync.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MensajeRepository mensajeRepository;

    public Mensaje save(Mensaje mensaje) {
        mensaje.setTimestamp(java.time.LocalDateTime.now());
        mensaje.setLeido(false);
        return mensajeRepository.save(mensaje);
    }

    public List<Mensaje> findChatHistory(Long senderId, Long recipientId) {
        return mensajeRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                senderId, recipientId, recipientId, senderId);
    }

    public long countNewMessages(Long senderId, Long recipientId) {
        return mensajeRepository.countByRecipientIdAndSenderIdAndLeidoFalse(recipientId, senderId);
    }
}
