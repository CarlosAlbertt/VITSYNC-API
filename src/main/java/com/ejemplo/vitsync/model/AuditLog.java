package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.AuditAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable audit record of a security- or privacy-relevant action.
 *
 * <p>Stored in {@code audit_logs} for GDPR Art. 30 / Ley 41/2002 traceability.
 * The actor is kept as the NIF string ({@code actorNif}) rather than a foreign
 * key so the trail survives user anonymisation/erasure (Art. 17): on erasure
 * the NIF is replaced by a pseudonym but the rows remain, decoupled from the
 * person (see {@code docs/GDPR_PROCEDURES.md}).</p>
 *
 * <p>These rows must never be updated or deleted in normal operation; they are
 * append-only legal evidence.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_actor", columnList = "actor_nif"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NIF of the actor (or "ANONYMOUS"/"SYSTEM" when no principal). */
    @Column(name = "actor_nif", length = 64)
    private String actorNif;

    /** The action performed. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditAction action;

    /** Optional id of the affected resource (e.g. patient/report id). */
    @Column(name = "target_id")
    private String targetId;

    /** Whether the action succeeded (e.g. LOGIN_FAILURE → false). */
    @Column(nullable = false)
    private boolean success;

    /** Client IP, captured from the request when available. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Free-form, non-sensitive detail (never store clinical content here). */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** When the action occurred. */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
