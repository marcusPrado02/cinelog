package com.cine.cinelog.infrastructure.messaging.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validador de segurança para payloads de eventos.
 * <p>
 * Garante que eventos não contenham dados sensíveis (PII) antes da publicação.
 * <p>
 * PR5: Event Security - PII Protection
 */
@Component
public class EventPayloadValidator {

    private static final Logger log = LoggerFactory.getLogger(EventPayloadValidator.class);

    /**
     * Padrões de campos sensíveis que NUNCA devem aparecer em eventos.
     * <p>
     * Validação é case-insensitive e busca por substring.
     */
    private static final Set<String> SENSITIVE_FIELD_PATTERNS = new HashSet<>(Arrays.asList(
            "password",
            "passwd",
            "secret",
            "token",
            "apikey",
            "api_key",
            "privatekey",
            "private_key",
            "ssn",
            "socialsecuritynumber",
            "creditcard",
            "credit_card",
            "cvv",
            "pin",
            "authorization",
            "bearer"));

    /**
     * Padrões de PII (Personally Identifiable Information) que requerem atenção.
     * <p>
     * Avisos são logados, mas não bloqueiam publicação (decisão de negócio).
     */
    private static final Set<String> PII_WARNING_PATTERNS = new HashSet<>(Arrays.asList(
            "email",
            "phone",
            "telephone",
            "address",
            "cpf",
            "rg",
            "passport"));

    /**
     * Valida se o payload contém dados sensíveis.
     * <p>
     * Lança exceção se encontrar campos proibidos.
     * Loga warning se encontrar PII.
     *
     * @param payload   Payload do evento a validar
     * @param eventType Tipo do evento (para logging)
     * @throws SecurityException se payload contiver dados sensíveis proibidos
     */
    public void validatePayload(Object payload, String eventType) {
        if (payload == null) {
            throw new IllegalArgumentException("Event payload cannot be null");
        }

        String payloadString = payload.toString().toLowerCase();

        // 1. Validar campos sensíveis proibidos
        for (String sensitivePattern : SENSITIVE_FIELD_PATTERNS) {
            if (payloadString.contains(sensitivePattern)) {
                log.error("SECURITY VIOLATION: Event payload contains sensitive field: {} in event type: {}",
                        sensitivePattern, eventType);
                throw new SecurityException(
                        "Event payload contains forbidden sensitive field: " + sensitivePattern);
            }
        }

        // 2. Validar PII (apenas warning)
        for (String piiPattern : PII_WARNING_PATTERNS) {
            if (payloadString.contains(piiPattern)) {
                log.warn("PII WARNING: Event payload may contain PII field: {} in event type: {}. " +
                        "Ensure this is intentional and complies with privacy policies.",
                        piiPattern, eventType);
            }
        }

        // 3. Validar via reflection (mais rigoroso)
        validateFieldsViaReflection(payload, eventType);

        log.debug("Event payload validated successfully for event type: {}", eventType);
    }

    /**
     * Valida campos via reflection para detecção mais rigorosa.
     * <p>
     * Analisa todos os campos do objeto para detectar nomes suspeitos.
     *
     * @param payload   Payload a validar
     * @param eventType Tipo do evento
     */
    private void validateFieldsViaReflection(Object payload, String eventType) {
        Class<?> payloadClass = payload.getClass();
        Field[] fields = payloadClass.getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName().toLowerCase();

            // Validar sensitive patterns em nomes de campos
            for (String sensitivePattern : SENSITIVE_FIELD_PATTERNS) {
                if (fieldName.contains(sensitivePattern)) {
                    log.error("SECURITY VIOLATION: Event payload has field with sensitive name: {} in event type: {}",
                            field.getName(), eventType);
                    throw new SecurityException(
                            "Event payload has field with forbidden sensitive name: " + field.getName());
                }
            }

            // Warning para PII em nomes de campos
            for (String piiPattern : PII_WARNING_PATTERNS) {
                if (fieldName.contains(piiPattern)) {
                    log.warn("PII WARNING: Event payload has field with PII name: {} in event type: {}",
                            field.getName(), eventType);
                }
            }
        }
    }

    /**
     * Trunca string para tamanho máximo seguro.
     * <p>
     * Útil para campos de texto livre (comentários, descrições).
     *
     * @param value     String a truncar
     * @param maxLength Tamanho máximo permitido
     * @return String truncada (com "..." se truncada)
     */
    public String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Mascara parte de uma string sensível.
     * <p>
     * Exemplo: "user@example.com" → "us**@ex****e.com"
     *
     * @param value        String a mascarar
     * @param visibleChars Número de caracteres visíveis no início/fim
     * @return String mascarada
     */
    public String mask(String value, int visibleChars) {
        if (value == null || value.length() <= visibleChars * 2) {
            return value;
        }

        String start = value.substring(0, visibleChars);
        String end = value.substring(value.length() - visibleChars);
        int maskedLength = value.length() - (visibleChars * 2);

        return start + "*".repeat(Math.min(maskedLength, 4)) + end;
    }

    /**
     * Retorna set imutável de padrões sensíveis para documentação.
     *
     * @return Set de padrões sensíveis
     */
    public Set<String> getSensitivePatterns() {
        return Set.copyOf(SENSITIVE_FIELD_PATTERNS);
    }

    /**
     * Retorna set imutável de padrões de PII para documentação.
     *
     * @return Set de padrões de PII
     */
    public Set<String> getPiiPatterns() {
        return Set.copyOf(PII_WARNING_PATTERNS);
    }
}
