package com.ejemplo.vitsync.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para JwtUtil.
 *
 * Verifica la generación, extracción de claims y validación de tokens JWT.
 * No necesita Spring Context — es un test unitario puro.
 */
@DisplayName("JwtUtil — Generación y Validación de Tokens")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Secret de test (debe tener al menos 64 caracteres para HS256)
    private static final String TEST_SECRET = "testsecretkeymustbelongenoughforhs256algorithmtestsecretkeymustbelongenoughforhs256algorithm";
    private static final long EXPIRATION_TIME = 86400000L; // 24 horas

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inyectar valores directamente sin levantar Spring
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", EXPIRATION_TIME);
    }

    // ─── Generación de Token ─────────────────────────────────────────

    @Test
    @DisplayName("Genera un token JWT no nulo y no vacío")
    void generateToken_returnsNonEmptyString() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");

        assertNotNull(token, "El token no debería ser null");
        assertFalse(token.isEmpty(), "El token no debería estar vacío");
        assertTrue(token.split("\\.").length == 3, "El token debe tener 3 partes (header.payload.signature)");
    }

    @Test
    @DisplayName("El token generado contiene el NIF como subject")
    void generateToken_containsNifAsSubject() {
        String nif = "12345678A";
        String token = jwtUtil.generateToken(nif, "PACIENTE");

        String extractedNif = jwtUtil.extractNif(token);
        assertEquals(nif, extractedNif, "El NIF extraído debe coincidir con el original");
    }

    @Test
    @DisplayName("El token generado contiene el rol en los claims")
    void generateToken_containsRole() {
        String token = jwtUtil.generateToken("12345678A", "ADMIN");

        String role = jwtUtil.extractRole(token);
        assertEquals("ADMIN", role, "El rol extraído debe ser ADMIN");
    }

    // ─── Extracción de Claims ────────────────────────────────────────

    @Test
    @DisplayName("Extrae correctamente la fecha de expiración")
    void extractExpiration_returnsValidDate() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");

        assertNotNull(jwtUtil.extractExpiration(token), "La fecha de expiración no debería ser null");
        assertTrue(jwtUtil.extractExpiration(token).getTime() > System.currentTimeMillis(),
                "La expiración debe ser en el futuro");
    }

    @Test
    @DisplayName("Distingue correctamente entre roles PACIENTE, MEDICO y ADMIN")
    void extractRole_distinguishesRoles() {
        String tokenPaciente = jwtUtil.generateToken("11111111A", "PACIENTE");
        String tokenMedico = jwtUtil.generateToken("22222222B", "MEDICO");
        String tokenAdmin = jwtUtil.generateToken("33333333C", "ADMIN");

        assertEquals("PACIENTE", jwtUtil.extractRole(tokenPaciente));
        assertEquals("MEDICO", jwtUtil.extractRole(tokenMedico));
        assertEquals("ADMIN", jwtUtil.extractRole(tokenAdmin));
    }

    // ─── Validación ─────────────────────────────────────────────────

    @Test
    @DisplayName("Valida un token con NIF correcto")
    void validateToken_withCorrectNif_returnsTrue() {
        String nif = "12345678A";
        String token = jwtUtil.generateToken(nif, "PACIENTE");

        assertTrue(jwtUtil.validateToken(token, nif), "Token con NIF correcto debe ser válido");
    }

    @Test
    @DisplayName("Rechaza un token con NIF incorrecto")
    void validateToken_withWrongNif_returnsFalse() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");

        assertFalse(jwtUtil.validateToken(token, "99999999Z"),
                "Token con NIF incorrecto debe ser inválido");
    }

    @Test
    @DisplayName("Rechaza un token expirado")
    void validateToken_withExpiredToken_throwsException() {
        // Crear JwtUtil con expiración de 0ms (token expira al instante)
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(expiredJwtUtil, "expirationTime", 0L);

        String token = expiredJwtUtil.generateToken("12345678A", "PACIENTE");

        // Un token expirado debe lanzar excepción al intentar parsearlo
        assertThrows(ExpiredJwtException.class,
                () -> expiredJwtUtil.validateToken(token, "12345678A"),
                "Debe lanzar ExpiredJwtException para tokens expirados");
    }

    @Test
    @DisplayName("Rechaza un token con formato inválido")
    void validateToken_withMalformedToken_throwsException() {
        assertThrows(MalformedJwtException.class,
                () -> jwtUtil.extractNif("esto.no.es.un.jwt"),
                "Debe lanzar MalformedJwtException para tokens inválidos");
    }

    @Test
    @DisplayName("Rechaza un token manipulado (firma alterada)")
    void validateToken_withTamperedToken_throwsException() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");
        // Alterar el último carácter de la firma
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThrows(Exception.class,
                () -> jwtUtil.validateToken(tampered, "12345678A"),
                "Debe lanzar excepción para tokens con firma alterada");
    }
}
