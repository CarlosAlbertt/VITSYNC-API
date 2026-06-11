package com.ejemplo.vitsync.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil} (RS256). No Spring context required: the RSA
 * key pair is injected via reflection and {@code initKeys()} is invoked
 * manually.
 */
@DisplayName("JwtUtil — RS256 token generation and validation")
class JwtUtilTest {

    // Par de claves RSA de TEST (mismas que src/test/resources/application.properties)
    private static final String TEST_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5CyMwHrNNmL5Wl8LF44EIg6/CcrBeb4Yk0u/Kua145ElZWTJLaE2e5YCOmr3idlOEtDxl06X6S6urNp3Mf+wGOtxbngXXWjhzUjaW3rOjVUfGbqTSAAfc0EedQXghGn3FG/sH92pxcwEZ6xV37M7cM/5oWqz95Bmn5XFHx5RV3mj6parnf+l172jGFLInWjpAJW8pNQEkypzd1FbCTp++EJet/tXsP/URSrUMZ+WSyewN0E8qXQulA0sYGhp8l3VLL8NwgvR/7IGbJoWkN4PuhjlA7yZuOolGpE8y7geMBWSakEAKu2LSfN1Gt5b0gmSpJYlea0bS9BClrk8IPy3JAgMBAAECggEALzqHfaWoT/rXQdS0MrvRWDH8Lx4Eo+XFECsCZvSjMQLbMcHRU2vIu/CJslwOcPmQcYNrEvZFG7AqnaVv+xz/ScvGKGAZz5BIbi6injkzElIW4q+kw7CcUSCb1qg6GADh7ugoVy0v7srSkiHtNdGsLonauhnCo36PicnG8vIeixes0XZgId78c9sjO71i+vOw8TYqIvIRt33S99U27zo8mbh9k5eUrt15H/z2QSMhDkm6iOQFHBWVmLTmFDNaPSEP+AcD4mg2Dh1o4jQkX4It14G7BAG23drMeiMQQSUm5oOfNAc0Bv9zypG/jHWxq5+Y5ZhRDIwEKFWKfg3a3YPlnQKBgQDn+2LZokxUKIyHvwlgpBZeUVKvaaM0HCVn/ImehB9j7L7SbT6FmeycwjZhrESn+Fo8evfaSwxKJjFchHtLSeMKhULZHGCaR1n4g0kMicXtoCTZHTyFrEdn+sLfb+FCTWW+m3MyokCP2ULVvpvlkGk/m/EgDgDH2UBxLVjXI6gdWwKBgQDMM6k2H7QTXmFwYzbdQPxjZTfMyUETB0ZjNloeo6LU+K5zW1U5KK3q72i2Jl9K/QgjPH0OtmkzwUuu7F13ohJa7ney45FB/SM1vCjazEJlz4rbumQWMkZ5TH/0ohuxF/TKzzgcLIolsc6ioBJLPkbGa5moIFP09ANRDjTa//VWqwKBgQDbqtXd06uHfaYk3KcalgaAZW1woQ1jyMs6/o2qRt4alxHS3JN5m1nMzrMEJkYU8D0yTBbq5GnMxQG049aEYoDVc37ra82mCa6OfnLrpoKAE0cROHgY9BvhwDhLr/uT9wpDRZv99FpCXK7HC+k/plGjZB0eB2SB2Z0GDrSzdBY7RQKBgGmqvgAk7bEsIK3gmU5qx2/Du9k7t3HaTOEgCgha0vLz8In/FB2s4Dp3Qq8nMh6Cy0g4j9oiKFRAzSIqa79xXaAyUDyAp/UGwcaXpGh8VEuM1yUW0Z3uzCsOnBQCIuREKkccbcOehKo21V+wB2dqRYN9wJiQigFyl5jFCjLdSp5bAoGAMdNNpRGFP/6lqu7215sK0TyFWU8bk1ztVDxjC0twROitutlrPmPWzsM6pMXm6Gnj0age3IxaLTNC9YoIWfukLwU9kxCrtLR+XnH2ycYGt0BW+pehpuo+zRaNqXohOijYQ8safOUcwbzJgmL1inW9N4A0zdj4MEZ5O9BjvE5cXxI=";
    private static final String TEST_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuQsjMB6zTZi+VpfCxeOBCIOvwnKwXm+GJNLvyrmteORJWVkyS2hNnuWAjpq94nZThLQ8ZdOl+kurqzadzH/sBjrcW54F11o4c1I2lt6zo1VHxm6k0gAH3NBHnUF4IRp9xRv7B/dqcXMBGesVd+zO3DP+aFqs/eQZp+VxR8eUVd5o+qWq53/pde9oxhSyJ1o6QCVvKTUBJMqc3dRWwk6fvhCXrf7V7D/1EUq1DGflksnsDdBPKl0LpQNLGBoafJd1Sy/DcIL0f+yBmyaFpDeD7oY5QO8mbjqJRqRPMu4HjAVkmpBACrti0nzdRreW9IJkqSWJXmtG0vQQpa5PCD8tyQIDAQAB";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = newJwtUtil(900000L);
    }

    private JwtUtil newJwtUtil(long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "privateKeyBase64", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(util, "publicKeyBase64", TEST_PUBLIC_KEY);
        ReflectionTestUtils.setField(util, "accessExpirationMs", expirationMs);
        ReflectionTestUtils.invokeMethod(util, "initKeys");
        return util;
    }

    @Test
    @DisplayName("Generates a non-empty 3-part JWT")
    void generateToken_returnsNonEmptyString() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Uses RS256 in the header, not HS256")
    void generateToken_usesRs256() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");
        String headerJson = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[0]));
        assertTrue(headerJson.contains("RS256"), "El header debe declarar alg RS256");
        assertFalse(headerJson.contains("HS256"));
    }

    @Test
    @DisplayName("Stores the NIF as subject")
    void generateToken_containsNifAsSubject() {
        String nif = "12345678A";
        String token = jwtUtil.generateToken(nif, "PACIENTE");
        assertEquals(nif, jwtUtil.extractNif(token));
    }

    @Test
    @DisplayName("Stores the role claim")
    void generateToken_containsRole() {
        String token = jwtUtil.generateToken("12345678A", "ADMIN");
        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Extracts a future expiration date")
    void extractExpiration_returnsValidDate() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");
        assertNotNull(jwtUtil.extractExpiration(token));
        assertTrue(jwtUtil.extractExpiration(token).getTime() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("Distinguishes PACIENTE/MEDICO/ADMIN roles")
    void extractRole_distinguishesRoles() {
        assertEquals("PACIENTE", jwtUtil.extractRole(jwtUtil.generateToken("11111111H", "PACIENTE")));
        assertEquals("MEDICO", jwtUtil.extractRole(jwtUtil.generateToken("22222222J", "MEDICO")));
        assertEquals("ADMIN", jwtUtil.extractRole(jwtUtil.generateToken("33333333P", "ADMIN")));
    }

    @Test
    @DisplayName("Valid token with matching NIF → true")
    void validateToken_withCorrectNif_returnsTrue() {
        String nif = "12345678A";
        String token = jwtUtil.generateToken(nif, "PACIENTE");
        assertTrue(jwtUtil.validateToken(token, nif));
    }

    @Test
    @DisplayName("Token with wrong NIF → false")
    void validateToken_withWrongNif_returnsFalse() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");
        assertFalse(jwtUtil.validateToken(token, "99999999R"));
    }

    @Test
    @DisplayName("Expired token → false (no exception leaked)")
    void validateToken_withExpiredToken_returnsFalse() {
        JwtUtil expired = newJwtUtil(0L);
        String token = expired.generateToken("12345678A", "PACIENTE");
        assertFalse(expired.validateToken(token, "12345678A"));
    }

    @Test
    @DisplayName("Malformed token → false")
    void validateToken_withMalformedToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("esto.no.es.un.jwt", "12345678A"));
    }

    @Test
    @DisplayName("Tampered signature → false")
    void validateToken_withTamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("12345678A", "PACIENTE");
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");
        assertFalse(jwtUtil.validateToken(tampered, "12345678A"));
    }
}
