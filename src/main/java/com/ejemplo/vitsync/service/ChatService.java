package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.Auditable;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.model.Mensaje;
import com.ejemplo.vitsync.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MensajeRepository mensajeRepository;

    public Mensaje save(Mensaje mensaje) {
        mensaje.setTimestamp(java.time.LocalDateTime.now());
        mensaje.setLeido(false);
        return mensajeRepository.save(mensaje);
    }

    @Auditable(action = AuditAction.VIEW_CHAT, targetIdIndex = 1)
    public List<Mensaje> findChatHistory(Long senderId, Long recipientId) {
        return mensajeRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                senderId, recipientId, recipientId, senderId);
    }

    public long countNewMessages(Long senderId, Long recipientId) {
        return mensajeRepository.countByRecipientIdAndSenderIdAndLeidoFalse(recipientId, senderId);
    }
}
