package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.RefreshToken;
import com.ejemplo.vitsync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} persistence and revocation.
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a token by the SHA-256 hash of its raw value.
     *
     * @param tokenHash hex-encoded SHA-256 of the presented token
     * @return the matching token, if any
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes every active refresh token of a user (logout-all / GDPR delete).
     *
     * @param user owner whose sessions are terminated
     * @return number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    /**
     * Housekeeping: removes tokens expired before the given instant.
     *
     * @param now cutoff instant
     * @return number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") LocalDateTime now);
}
