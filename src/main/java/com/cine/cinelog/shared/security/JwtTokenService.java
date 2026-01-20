package com.cine.cinelog.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

/**
 * Serviço responsável por operação de JwtToken.
 * Implementa o caso de uso de operação aplicando regras de negócio e políticas
 * de domínio.
 * 
 * <p>
 * Este serviço coordena as operações necessárias e garante
 * a consistência dos dados através de validações e políticas.
 * </p>
 * 
 * @since 1.0
 * @see JwtTokenService
 */
@Component
public class JwtTokenService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${cinelog.security.jwt.secret}") String secret,
            @Value("${cinelog.security.jwt.expiration-seconds:3600}") long expirationSeconds) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationSeconds = expirationSeconds;
    }

    @Measured("cinelog.security.jwt.generate_token")
    @AlertIfSlow(thresholdMs = 200)
    public String generateToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    @Measured("cinelog.security.jwt.extract_subject")
    @AlertIfSlow(thresholdMs = 200)
    public String extractSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
