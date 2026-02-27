package com.cine.cinelog.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;

/**
 * A08:2025 — Tokens HMAC para ações seguras (reset password, confirmação de
 * email, etc).
 *
 * <p>
 * Gera tokens autocontidos com assinatura HMAC que incluem:
 * propósito (purpose), ID do usuário, expiração, e hash de integridade.
 * Isso impede que um atacante forge ou reutilize tokens de ação.
 * </p>
 *
 * <h3>Cenários protegidos:</h3>
 * <ul>
 * <li>Atacante modifica userId no link de reset → HMAC inválido</li>
 * <li>Atacante reutiliza token expirado → verificação de expiração falha</li>
 * <li>Atacante troca purpose (confirm→reset) → propósito no HMAC não bate</li>
 * </ul>
 *
 * <h3>Formato do token:</h3>
 * 
 * <pre>
 * Base64(purpose:userId:expiresEpochSeconds:hmac)
 * </pre>
 *
 * @since 1.3.0
 */
@Service
@Slf4j
public class SecureActionTokenService {

    private final IntegrityService integrityService;
    private final long defaultExpirationSeconds;

    public SecureActionTokenService(
            IntegrityService integrityService,
            @Value("${cinelog.security.action-token.expiration-seconds:3600}") long defaultExpirationSeconds) {
        this.integrityService = integrityService;
        this.defaultExpirationSeconds = defaultExpirationSeconds;
    }

    /**
     * Gera um token HMAC para uma ação.
     *
     * @param purpose propósito do token (ex: "PASSWORD_RESET", "EMAIL_CONFIRM")
     * @param userId  ID do usuário alvo
     * @return token Base64 URL-safe
     */
    public String generateToken(String purpose, Long userId) {
        return generateToken(purpose, userId, defaultExpirationSeconds);
    }

    /**
     * Gera um token HMAC com expiração customizada.
     */
    public String generateToken(String purpose, Long userId, long expirationSeconds) {
        long expiresAt = Instant.now().plusSeconds(expirationSeconds).getEpochSecond();

        String payload = purpose + ":" + userId + ":" + expiresAt;
        String hmac = integrityService.sign(payload);

        String token = payload + ":" + hmac;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes());
    }

    /**
     * Verifica e decodifica um token HMAC.
     *
     * @param token   token Base64 recebido
     * @param purpose propósito esperado (deve bater com o gerado)
     * @return resultado da validação com userId extraído
     * @throws InvalidActionTokenException se token inválido, expirado ou adulterado
     */
    public ActionTokenPayload verifyToken(String token, String purpose) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token));
            String[] parts = decoded.split(":", 4);

            if (parts.length != 4) {
                throw new InvalidActionTokenException("Token malformado");
            }

            String tokenPurpose = parts[0];
            Long userId = Long.parseLong(parts[1]);
            long expiresAt = Long.parseLong(parts[2]);
            String hmac = parts[3];

            // Verificar propósito
            if (!purpose.equals(tokenPurpose)) {
                log.warn("A08:2025 — Token com propósito diferente: expected={}, got={}",
                        purpose, tokenPurpose);
                throw new InvalidActionTokenException("Propósito do token não corresponde");
            }

            // Verificar expiração
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new InvalidActionTokenException("Token expirado");
            }

            // Verificar HMAC (integridade)
            String payload = tokenPurpose + ":" + userId + ":" + expiresAt;
            if (!integrityService.verify(payload, hmac)) {
                log.error("A08:2025 — Token HMAC inválido — possível adulteração");
                throw new InvalidActionTokenException("Assinatura do token inválida");
            }

            return new ActionTokenPayload(tokenPurpose, userId, Instant.ofEpochSecond(expiresAt));

        } catch (InvalidActionTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidActionTokenException("Token inválido: " + e.getMessage());
        }
    }

    /**
     * Dados extraídos de um token verificado com sucesso.
     */
    public record ActionTokenPayload(String purpose, Long userId, Instant expiresAt) {
    }

    /**
     * Token de ação inválido ou adulterado.
     */
    public static class InvalidActionTokenException extends RuntimeException {
        public InvalidActionTokenException(String message) {
            super(message);
        }
    }
}
