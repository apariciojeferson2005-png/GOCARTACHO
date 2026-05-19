package com.gocartacho.gocartacho.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Utilidad criptográfica encargada de generar, firmar y validar tokens JWT.
 * Valida la existencia y robustez de la clave secreta inyectada desde las
 * variables de entorno.
 */
@Slf4j
@Component
public class UtilidadJwt {

    // Secreto inyectable
    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String secretKey;
    private static final long EXPIRATION_TIME = 86400000; // 1 día

    @PostConstruct
    public void validateSecret() {
        if (secretKey == null || secretKey.trim().isEmpty() || secretKey.contains("${JWT_SECRET}")) {
            log.error("CRÍTICO: La clave secreta JWT no está configurada o es inválida.");
            throw new IllegalStateException("JWT Secret must be provided via environment variable JWT_SECRET");
        }
        if (secretKey.length() < 32) {
            log.warn("ADVERTENCIA: La clave secreta JWT es demasiado corta (mínimo 32 caracteres para HS256).");
        }
        // Evitar el secreto por defecto mencionado en el reporte si existiera
        if (secretKey.equals("gocartachoDefaultSecretKey123456789012345678901234567890")) {
            log.error("CRÍTICO: Se está utilizando el secreto de desarrollo en un entorno configurado.");
            throw new IllegalStateException("Security breach: Default development secret detected.");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
