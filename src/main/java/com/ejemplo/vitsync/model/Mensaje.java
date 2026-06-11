package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.converter.SensitiveDataConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Chat message between patient and clinician.
 *
 * <p>{@code content} is medical-conversation data and is encrypted at rest
 * with AES-256-GCM ({@link SensitiveDataConverter}).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Entity
@Table(name = "mensajes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long recipientId;

    /** Texto del mensaje. Dato clínico: cifrado en reposo (AES-256-GCM). */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean leido;
}
