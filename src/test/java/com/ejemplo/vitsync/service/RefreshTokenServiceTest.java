package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.model.RefreshToken;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenService}: issuance, rotation
 * (verify-and-revoke), revocation and expired-token housekeeping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    private static final long SEVEN_DAYS_MS = 604_800_000L;

    @Mock RefreshTokenRepository refreshTokenRepository;
    @InjectMocks RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        // @Value no se resuelve fuera del contexto Spring: lo fijamos a mano
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", SEVEN_DAYS_MS);
        user = new User();
        user.setId(1L);
    }

    // ---- create ----

    @Test
    @DisplayName("create: devuelve token aleatorio y persiste solo su hash")
    void create_persistsHashNotRawToken() {
        String raw = refreshTokenService.create(user);

        assertNotNull(raw);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertNotEquals(raw, saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length()); // SHA-256 en hex
        assertSame(user, saved.getUser());
        assertFalse(saved.isRevoked());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("create: la caducidad respeta jwt.refresh-expiration")
    void create_expiryMatchesConfiguredLifetime() {
        refreshTokenService.create(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        LocalDateTime expected = LocalDateTime.now().plusSeconds(SEVEN_DAYS_MS / 1000);
        // Margen de 1 min para absorber el tiempo de ejecución del test
        assertTrue(captor.getValue().getExpiresAt().isAfter(expected.minusMinutes(1)));
        assertTrue(captor.getValue().getExpiresAt().isBefore(expected.plusMinutes(1)));
    }

    @Test
    @DisplayName("create: dos llamadas → tokens distintos (SecureRandom)")
    void create_generatesUniqueTokens() {
        String first = refreshTokenService.create(user);
        String second = refreshTokenService.create(user);
        assertNotEquals(first, second);
    }

    // ---- verifyAndRevoke (rotación) ----

    @Test
    @DisplayName("verifyAndRevoke: token válido → lo revoca y lo devuelve")
    void verifyAndRevoke_valid_revokesAndReturns() {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verifyAndRevoke("raw");

        assertSame(token, result);
        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("verifyAndRevoke: token desconocido → BusinessException")
    void verifyAndRevoke_unknown_throws() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> refreshTokenService.verifyAndRevoke("raw"));
    }

    @Test
    @DisplayName("verifyAndRevoke: token ya revocado (replay) → BusinessException")
    void verifyAndRevoke_alreadyRevoked_throws() {
        RefreshToken token = RefreshToken.builder()
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () -> refreshTokenService.verifyAndRevoke("raw"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyAndRevoke: token caducado → BusinessException")
    void verifyAndRevoke_expired_throws() {
        RefreshToken token = RefreshToken.builder()
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () -> refreshTokenService.verifyAndRevoke("raw"));
        verify(refreshTokenRepository, never()).save(any());
    }

    // ---- revoke / revokeAll / purgeExpired ----

    @Test
    @DisplayName("revoke: token conocido → marcado como revocado")
    void revoke_known_marksRevoked() {
        RefreshToken token = RefreshToken.builder().revoked(false).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        refreshTokenService.revoke("raw");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("revoke: token desconocido → idempotente, sin error ni save")
    void revoke_unknown_isIdempotent() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> refreshTokenService.revoke("raw"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeAll: delega y devuelve nº de sesiones revocadas")
    void revokeAll_delegates() {
        when(refreshTokenRepository.revokeAllByUser(user)).thenReturn(3);
        assertEquals(3, refreshTokenService.revokeAll(user));
    }

    @Test
    @DisplayName("purgeExpired: borra tokens caducados a fecha actual")
    void purgeExpired_deletesExpired() {
        refreshTokenService.purgeExpired();
        verify(refreshTokenRepository).deleteExpiredBefore(any(LocalDateTime.class));
    }
}
