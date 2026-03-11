package com.cine.cinelog.features.auth.service;

import com.cine.cinelog.features.auth.persistence.entity.RefreshTokenEntity;
import com.cine.cinelog.features.auth.persistence.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * A07:2025 — Gerenciamento de Refresh Tokens com rotation e detecção de reuso.
 *
 * <p>
 * <b>Refresh Token Rotation:</b> cada vez que um refresh token é usado, ele é
 * revogado e um NOVO token é emitido na mesma família. Isso garante que cada
 * refresh token só pode ser usado UMA vez.
 * </p>
 *
 * <p>
 * <b>Detecção de Reuso (Token Replay):</b> se um token JÁ REVOGADO for
 * apresentado, significa que o token original foi comprometido (roubado) e
 * OUTRA pessoa já o usou. Nesse caso, TODA a família de tokens é revogada,
 * forçando re-autenticação completa.
 * </p>
 *
 * <pre>
 * Login → AccessToken + RefreshToken(v1, family=ABC)
 *   │
 *   ▼ (AccessToken expira após 1h)
 * POST /api/auth/refresh { refreshToken: v1 }
 *   → Revoga v1, emite v2 (family=ABC) + novo AccessToken
 *   │
 *   ▼ (AccessToken expira novamente)
 * POST /api/auth/refresh { refreshToken: v2 }
 *   → Revoga v2, emite v3 (family=ABC) + novo AccessToken
 *
 * ⚠️ Se atacante rouba v1 e tenta usar:
 * POST /api/auth/refresh { refreshToken: v1 }
 *   → v1 já revogado! TODA família ABC é invalidada (v2 também).
 *   → Usuário legítimo é forçado a fazer login novamente.
 *   → Atacante não consegue continuar.
 * </pre>
 */
@Service
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${cinelog.security.auth.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        log.info("RefreshTokenService: expiração={}s ({}d)",
                refreshTokenExpirationSeconds, refreshTokenExpirationSeconds / 86400);
    }

    /**
     * Sanitiza valores antes de logar para evitar log injection (quebra de linha, etc).
     */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        // Remove quebras de linha e caracteres de controle que poderiam quebrar o formato do log
        return value
                .replace('\n', ' ')
                .replace('\r', ' ');
    }

    /**
     * Cria um novo refresh token para o usuário (usado no login e no registro).
     *
     * @param userId    ID do usuário
     * @param email     email do usuário
     * @param clientIp  IP do cliente
     * @param userAgent User-Agent do cliente
     * @return o token opaco (UUID) gerado
     */
    @Transactional
    public String createRefreshToken(Long userId, String email, String clientIp, String userAgent) {
        String token = UUID.randomUUID().toString();
        String family = UUID.randomUUID().toString();

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setToken(token);
        entity.setTokenFamily(family);
        entity.setUserId(userId);
        entity.setUserEmail(email);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpirationSeconds));
        entity.setClientIp(clientIp);
        entity.setUserAgent(truncate(userAgent, 512));

        refreshTokenRepository.save(entity);

        log.debug("Refresh token criado: userId={}", userId);
        return token;
    }

    /**
     * Rotaciona um refresh token: revoga o atual e emite um novo na mesma família.
     *
     * @param currentToken o refresh token atual
     * @param clientIp     IP do cliente
     * @param userAgent    User-Agent do cliente
     * @return resultado da rotação (contém novo token, userId e email)
     * @throws RefreshTokenException se o token é inválido, expirado ou já revogado
     */
    @Transactional
    public RotationResult rotateRefreshToken(String currentToken, String clientIp, String userAgent) {
        RefreshTokenEntity existing = refreshTokenRepository.findByToken(currentToken)
                .orElseThrow(() -> new RefreshTokenException("Refresh token não encontrado."));

        // ⚠️ DETECÇÃO DE REUSO: token já revogado = comprometimento
        if (existing.isRevoked()) {
            log.error("A07:2025 — REFRESH TOKEN REUSE DETECTED! "
                    + "userId={}, ip={}. Revogando toda a família.",
                    existing.getUserId(), sanitizeForLog(clientIp));
            // Revogar TODA a família — ataque em andamento
            refreshTokenRepository.revokeAllByFamily(existing.getTokenFamily(), Instant.now());
            throw new RefreshTokenException(
                    "Refresh token foi reutilizado — sessão comprometida. Faça login novamente.");
        }

        // Token expirado
        if (existing.isExpired()) {
            existing.setRevoked(true);
            existing.setRevokedAt(Instant.now());
            refreshTokenRepository.save(existing);
            throw new RefreshTokenException("Refresh token expirado.");
        }

        // Revogar o token atual
        String newToken = UUID.randomUUID().toString();
        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedBy(newToken);
        refreshTokenRepository.save(existing);

        // Criar novo token na MESMA família
        RefreshTokenEntity newEntity = new RefreshTokenEntity();
        newEntity.setToken(newToken);
        newEntity.setTokenFamily(existing.getTokenFamily()); // mesma família
        newEntity.setUserId(existing.getUserId());
        newEntity.setUserEmail(existing.getUserEmail());
        newEntity.setCreatedAt(Instant.now());
        newEntity.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpirationSeconds));
        newEntity.setClientIp(clientIp);
        newEntity.setUserAgent(truncate(userAgent, 512));
        refreshTokenRepository.save(newEntity);

        log.debug("Refresh token rotacionado: userId={}, family={}",
                existing.getUserId(), existing.getTokenFamily());

        return new RotationResult(newToken, existing.getUserId(), existing.getUserEmail());
    }

    /**
     * Revoga todos os tokens de um usuário (usado no logout e change-password).
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("Revogados {} refresh token(s) do userId={}", count, userId);
    }

    /**
     * Housekeeping: remove tokens expirados há mais de 1 dia (reduz tamanho da
     * tabela).
     * Executado diariamente às 02:00.
     *
     * A10:2025 — try-catch obrigatório em @Scheduled para evitar que uma exceção
     * transiente (ex.: DB temporariamente indisponível) mate o scheduler thread
     * e impeça todas as execuções futuras.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            Instant cutoff = Instant.now().minusSeconds(86400); // expirados há mais de 24h
            int deleted = refreshTokenRepository.deleteExpiredBefore(cutoff);
            if (deleted > 0) {
                log.info("Housekeeping: removidos {} refresh tokens expirados", deleted);
            }
        } catch (Exception ex) {
            log.error("A10:2025 — Falha no housekeeping de refresh tokens. "
                    + "Scheduler continuará na próxima execução.", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * Resultado de uma rotação de refresh token.
     */
    public record RotationResult(String newRefreshToken, Long userId, String userEmail) {
    }

    /**
     * Exceção de refresh token (inválido, expirado, reuso).
     */
    public static class RefreshTokenException extends RuntimeException {
        public RefreshTokenException(String message) {
            super(message);
        }
    }
}
