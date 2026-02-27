package com.cine.cinelog.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * A08:2025 — Serviço de integridade de dados via HMAC-SHA256.
 *
 * <p>
 * Gera e verifica assinaturas HMAC para detectar adulteração de dados críticos.
 * Usado para proteger registros no banco de dados contra modificação direta
 * (bypass da API),
 * e para assinar payloads de eventos Kafka.
 * </p>
 *
 * <h3>Cenários protegidos:</h3>
 * <ul>
 * <li>Admin malicioso altera role de usuário diretamente no DB → HMAC
 * invalidado</li>
 * <li>Payload de evento Kafka adulterado em trânsito → verificação falha</li>
 * <li>Registro de auditoria modificado → hash chain quebrada</li>
 * </ul>
 *
 * @since 1.3.0
 */
@Component
@Slf4j
public class IntegrityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secretKey;

    public IntegrityService(
            @Value("${cinelog.security.integrity.secret:${cinelog.security.jwt.secret}}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "A08:2025 — cinelog.security.integrity.secret deve ter no mínimo 32 caracteres");
        }
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Gera HMAC-SHA256 para um conteúdo.
     *
     * @param content dados a assinar (ex: concatenação de campos críticos)
     * @return assinatura em Base64 URL-safe
     */
    public String sign(String content) {
        Objects.requireNonNull(content, "Conteúdo para assinatura não pode ser null");
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IntegrityException("Falha ao gerar HMAC", e);
        }
    }

    /**
     * Verifica se a assinatura HMAC corresponde ao conteúdo.
     *
     * @param content   dados originais
     * @param signature assinatura HMAC a verificar
     * @return true se a assinatura é válida
     */
    public boolean verify(String content, String signature) {
        if (content == null || signature == null) {
            return false;
        }
        try {
            String expected = sign(content);
            // Comparação em tempo constante para evitar timing attack
            return constantTimeEquals(expected, signature);
        } catch (Exception e) {
            log.warn("A08:2025 — Falha na verificação de integridade", e);
            return false;
        }
    }

    /**
     * Gera conteúdo de integridade para entidade a partir de campos críticos.
     *
     * <p>
     * Concatena campos separados por pipe (|) para gerar string de assinatura.
     * </p>
     *
     * @param fields campos a incluir na assinatura
     * @return string concatenada para assinar
     */
    public String buildSignableContent(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(fields[i] != null ? fields[i].toString() : "null");
        }
        return sb.toString();
    }

    /**
     * Assina campos de uma entidade e retorna o HMAC.
     */
    public String signEntity(Object... criticalFields) {
        String content = buildSignableContent(criticalFields);
        return sign(content);
    }

    /**
     * Verifica integridade de uma entidade.
     */
    public boolean verifyEntity(String storedHmac, Object... criticalFields) {
        String content = buildSignableContent(criticalFields);
        return verify(content, storedHmac);
    }

    /**
     * Comparação em tempo constante para prevenir timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Exceção de falha de integridade.
     */
    public static class IntegrityException extends RuntimeException {
        public IntegrityException(String message) {
            super(message);
        }

        public IntegrityException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exceção de violação de integridade (tamper detected).
     */
    public static class TamperDetectedException extends RuntimeException {
        private final String entityType;
        private final String entityId;

        public TamperDetectedException(String entityType, String entityId) {
            super("A08:2025 — Violação de integridade detectada: " + entityType + "#" + entityId);
            this.entityType = entityType;
            this.entityId = entityId;
        }

        public String getEntityType() {
            return entityType;
        }

        public String getEntityId() {
            return entityId;
        }
    }
}
