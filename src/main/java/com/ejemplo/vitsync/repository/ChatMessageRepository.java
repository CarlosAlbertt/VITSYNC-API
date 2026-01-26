package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para acceder a la base de datos de mensajes.
 * Extiende JpaRepository para tener métodos CRUD básicos (save, findAll,
 * delete, etc.) gratis.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Busca el historial de chat entre dos usuarios (A y B).
     *
     * La lógica es: "Dame los mensajes donde..."
     * (Emisor es A Y Receptor es B) <-- Mensajes de A a B
     * O (OR)
     * (Emisor es B Y Receptor es A) <-- Mensajes de B a A
     *
     * OrderByTimestampAsc: Importante para que salgan en orden de llegada (antiguos
     * primero).
     *
     * @param senderId1    ID del primer participante (rol emisor en primer caso)
     * @param recipientId1 ID del segundo participante (rol receptor en primer caso)
     * @param senderId2    ID del segundo participante (rol emisor en segundo caso)
     * @param recipientId2 ID del primer participante (rol receptor en segundo caso)
     * @return Lista de mensajes ordenados cronológicamente.
     */
    public List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
            Long senderId1,
            Long recipientId1,
            Long senderId2,
            Long recipientId2);
}
