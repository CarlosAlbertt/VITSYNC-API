package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.model.RefreshToken;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues, rotates and revokes persistent refresh tokens.
 *
 * <p>Design decisions:</p>
 * <ul>
 *   <li>The raw token is 256 bits from {@link SecureRandom}, base64url-encoded.
 *       It is opaque (not a JWT): it carries no claims and is only meaningful
 *       against the database row holding its hash.</li>
 *   <li>Only the SHA-256 hash is persisted ({@code token_hash}); a DB dump
 *       cannot be replayed as a session.</li>
 *   <li><b>Rotation</b>: every successful refresh revokes the presented token
 *       and issues a new one. A replayed (already-revoked) token is rejected,
 *       which also acts as a theft-detection signal.</li>
 * </ul>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Refresh-token lifetime in milliseconds (default: 7 days). */
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates and persists a new refresh token for the user.
     *
     * @param user session owner
     * @return the RAW token to hand to the client (never stored server-side)
     */
    @Transactional
    public String create(User user) {
        // 256 bits de SecureRandom: imposible de adivinar por fuerza bruta
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a presented refresh token and rotates it: the old token is
     * revoked and a fresh one is issued for the same user.
     *
     * @param rawToken token presented by the client
     * @return the validated (now revoked) entity, with the owning user loaded
     * @throws BusinessException if the token is unknown, revoked or expired
     */
    @Transactional
    public RefreshToken verifyAndRevoke(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                // Mensaje único para token desconocido/revocado/caducado:
                // no damos pistas sobre cuál de los tres casos es
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token inválido");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return token;
    }

    /**
     * Revokes a single token (logout). Idempotent: unknown tokens are ignored
     * so logout never errors and never leaks token validity.
     *
     * @param rawToken token presented by the client
     */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Revokes every active session of the user (logout-all, password change,
     * GDPR erasure).
     *
     * @param user owner whose sessions are terminated
     * @return number of sessions revoked
     */
    @Transactional
    public int revokeAll(User user) {
        return refreshTokenRepository.revokeAllByUser(user);
    }

    /** Housekeeping: borra tokens caducados una vez al día (03:00). */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now());
    }

    /** Hex-encoded SHA-256 used as the storage form of tokens. */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 existe en toda JVM: si falta, el runtime está roto
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
