package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Persistent, revocable refresh token.
 *
 * <p>Access tokens are stateless and live only 15 minutes; session longevity
 * comes from this entity. Only the <b>SHA-256 hash</b> of the opaque token is
 * stored — never the raw value — so a database leak does not allow session
 * hijacking (same reasoning as password hashing).</p>
 *
 * <p>Lifecycle: created at login, rotated on every {@code /api/auth/refresh}
 * call (the used token is revoked and a new one issued), revoked at logout,
 * and bulk-revoked by {@code /api/auth/logout-all}.</p>
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
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
})
public class RefreshToken {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner of the session. LAZY: solo se necesita al rotar/revocar. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * SHA-256 hash (hex) of the opaque refresh token. The raw token is only
     * ever held by the client; lookups hash the presented value first.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Instant after which the token is unusable (7 days from issue). */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** {@code true} once consumed by rotation, logout or logout-all. */
    @Column(nullable = false)
    private boolean revoked;

    /** Issue instant — kept for audit/forensics. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Client IP at issue/refresh (proxied via X-Forwarded-For). Nullable for legacy rows. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Raw User-Agent of the client, for showing the device in "active sessions". */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Last time this session was used (login or refresh). */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
