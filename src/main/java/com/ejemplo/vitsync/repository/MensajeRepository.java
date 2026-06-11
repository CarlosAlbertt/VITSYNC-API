package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Obtener historial de chat entre dos usuarios
    List<Mensaje> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
            Long senderId1, Long recipientId1,
            Long senderId2, Long recipientId2);

    // Contar mensajes no leídos
    long countByRecipientIdAndSenderIdAndLeidoFalse(Long recipientId, Long senderId);

    // Todos los mensajes en los que participa un usuario (export/olvido RGPD)
    List<Mensaje> findBySenderIdOrRecipientId(Long senderId, Long recipientId);
}
