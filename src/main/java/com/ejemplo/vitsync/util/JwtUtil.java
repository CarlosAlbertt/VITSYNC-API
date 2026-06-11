package com.ejemplo.vitsync.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility responsible for issuing and validating access tokens.
 *
 * <p>Signature algorithm: <b>RS256</b> (RSA + SHA-256, asymmetric).
 * The token is signed with the RSA private key and verified with the
 * public key. This replaces the previous HS256 (symmetric) setup: with
 * HS256 a single shared secret both signs and verifies, so any component
 * (or attacker) holding it can forge tokens for any user — an unacceptable
 * risk for health data (GDPR Art. 32). With RS256 the private key can be
 * confined to this service while verifiers only ever need the public key.</p>
 *
 * <p>Key material is provided through environment variables as base64-encoded
 * DER: {@code JWT_PRIVATE_KEY} (PKCS#8) and {@code JWT_PUBLIC_KEY} (X.509).
 * Rotating keys is therefore a deployment-time operation: publish a new pair,
 * restart, and outstanding access tokens (max 15 minutes old) expire shortly
 * after.</p>
 *
 * <p>Token claims:</p>
 * <ul>
 *   <li>{@code sub} — the user's NIF, the stable unique login identifier.</li>
 *   <li>{@code role} — the user's role (ADMIN/MEDICO/PACIENTE); used by
 *       {@code JwtAuthenticationFilter} to build the granted authority.</li>
 *   <li>{@code iat}/{@code exp} — issue and expiry instants. Access tokens
 *       live 15 minutes; longevity is delegated to revocable refresh tokens
 *       stored in the database (see {@code RefreshTokenService}).</li>
 * </ul>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Component
public class JwtUtil {

    /** Base64-encoded DER (PKCS#8) RSA private key — signs access tokens. */
    @Value("${jwt.private-key}")
    private String privateKeyBase64;

    /** Base64-encoded DER (X.509) RSA public key — verifies access tokens. */
    @Value("${jwt.public-key}")
    private String publicKeyBase64;

    /** Access-token lifetime in milliseconds (default: 15 minutes). */
    @Value("${jwt.access-expiration:900000}")
    private long accessExpirationMs;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    /**
     * Decodes the RSA key pair once at startup so a malformed key fails fast
     * (at boot) instead of on the first login request.
     *
     * @throws IllegalStateException if either key cannot be decoded
     */
    @PostConstruct
    void initKeys() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64.trim())));
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64.trim())));
        } catch (Exception e) {
            // No exponemos detalle de la clave en el mensaje: solo que la
            // configuración es inválida (la traza completa va al log de boot)
            throw new IllegalStateException(
                    "Claves RSA de JWT inválidas: revisa JWT_PRIVATE_KEY/JWT_PUBLIC_KEY (base64 DER)", e);
        }
    }

    /**
     * Extracts the NIF (token subject) from a signed token.
     *
     * @param token compact JWS string
     * @return the NIF stored as {@code sub}
     * @throws JwtException if the token is invalid, expired or tampered with
     */
    public String extractNif(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiry instant from a signed token.
     *
     * @param token compact JWS string
     * @return expiration date of the token
     * @throws JwtException if the token is invalid or tampered with
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor: parses and verifies the token, then applies
     * the given resolver over its claims.
     *
     * @param token          compact JWS string
     * @param claimsResolver function mapping the verified claims to a value
     * @param <T>            resolved claim type
     * @return the resolved claim value
     * @throws JwtException if the token is invalid, expired or tampered with
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the token and verifies its RS256 signature against the public key.
     * jjwt 0.12 API: {@code parser().verifyWith(...)}.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** @return {@code true} if the token's {@code exp} is in the past. */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Issues a short-lived (15 min) RS256-signed access token.
     *
     * @param username the user's NIF (becomes the {@code sub} claim)
     * @param role     the user's role name (custom {@code role} claim)
     * @return compact JWS access token
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    /**
     * Builds and signs the access token with the RSA private key.
     * jjwt 0.12 API: {@code claims()/subject()/signWith(key, Jwts.SIG.RS256)}.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpirationMs))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Validates a token for a given user: signature, expiry and subject match.
     *
     * @param token compact JWS string
     * @param nif   expected subject
     * @return {@code true} only if signature is valid, not expired, and the
     *         subject equals {@code nif}
     */
    public Boolean validateToken(String token, String nif) {
        try {
            final String extractedNif = extractNif(token);
            return (extractedNif.equals(nif) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            // Token manipulado, caducado o malformado → inválido, sin excepción
            return false;
        }
    }

    /**
     * Extracts the {@code role} claim from a signed token.
     *
     * @param token compact JWS string
     * @return role name stored in the token (e.g. {@code "PACIENTE"})
     * @throws JwtException if the token is invalid or tampered with
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}
