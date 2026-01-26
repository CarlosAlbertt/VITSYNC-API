package com.ejemplo.vitsync.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entidad que representa un mensaje de chat en la base de datos.
 *
 * @Entity: Indica que esta clase se mapeará a una tabla en la base de datos.
 * @Data: De Lombok, genera automáticamente getters, setters, toString, etc.
 */
@Entity
@Data
@AllArgsConstructor // Genera constructor con todos los argumentos
@NoArgsConstructor // Genera constructor vacío (necesario para JPA)
public class ChatMessage {

    /**
     * Identificador único del mensaje.
     * 
     * @Id: Clave primaria.
     * @GeneratedValue: Se autogenera (autoincremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del usuario que envía el mensaje.
     */
    private Long senderId;

    /**
     * ID del usuario que recibe el mensaje.
     */
    private Long recipientId;

    /**
     * Contenido de texto del mensaje.
     */
    private String content;

    /**
     * Fecha y hora exacta del envío.
     * Útil para ordenar el historial cronológicamente.
     */
    private Timestamp timestamp;

}
