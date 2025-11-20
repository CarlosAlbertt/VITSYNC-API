package com.ejemplo.vitsync.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Clave secreta para firmar los tokens (debe estar en application.properties)
    @Value("${jwt.secret:miClaveSecretaSuperSeguraQueDebeSerMuyLarga123456789}")
    private String secret;

    // Tiempo de expiración del token (24 horas = 86400000 ms)
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    // Genera la clave de firma
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // GENERAR TOKEN
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    // Crear el token con claims y subject
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // Username del usuario
                .setIssuedAt(now) // Fecha de creación
                .setExpiration(expiryDate) // Fecha de expiración
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Firma con algoritmo HS256
                .compact();
    }

    // VALIDAR TOKEN
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // EXTRAER USERNAME DEL TOKEN
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // EXTRAER FECHA DE EXPIRACIÓN
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // EXTRAER UN CLAIM ESPECÍFICO
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // EXTRAER TODOS LOS CLAIMS
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // VERIFICAR SI EL TOKEN EXPIRÓ
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}