package com.cine.cinelog.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilitário para mascaramento de dados sensíveis em logs e auditoria.
 *
 * <p>
 * Previne exposição de PII e credenciais (OWASP A02).
 * </p>
 *
 * <p>
 * Campos mascarados automaticamente:
 * <ul>
 * <li>password, senha</li>
 * <li>token, secret, authorization</li>
 * <li>credit_card, creditCard, cvv</li>
 * <li>ssn, cpf</li>
 * </ul>
 *
 * @since 1.1
 */
@Component
public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "senha", "token", "secret", "authorization",
            "credit_card", "creditcard", "cvv", "ssn", "cpf",
            "refresh_token", "refreshtoken", "access_token", "accesstoken");

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(Bearer\\s+)[\\w.~+/=-]+",
            Pattern.CASE_INSENSITIVE);

    /**
     * Mascara valores de campos sensíveis em texto livre (ex: toString de objetos).
     * Procura padrões {@code key=value} e {@code "key":"value"}.
     */
    public String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // Mascara campos key=value e "key":"value"
        for (String key : SENSITIVE_KEYS) {
            // key=value (log/toString)
            result = result.replaceAll(
                    "(?i)(" + Pattern.quote(key) + "\\s*[=:]\\s*)([^\\s,;}&\"']+)",
                    "$1***MASKED***");
            // "key":"value" (JSON)
            result = result.replaceAll(
                    "(?i)(\"" + Pattern.quote(key) + "\"\\s*:\\s*\")([^\"]*)(\")",
                    "$1***MASKED***$3");
        }

        // Mascara Bearer tokens
        result = BEARER_PATTERN.matcher(result).replaceAll("$1***MASKED***");

        return result;
    }

    /**
     * Mascara um endereço de email preservando os 2 primeiros caracteres do local
     * part.
     *
     * @param email endereço completo
     * @return email mascarado (ex: {@code us***@example.com})
     */
    public String maskEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() > 2
                ? local.substring(0, 2) + "***"
                : "***";
        return masked + "@" + parts[1];
    }

    /**
     * Verifica se uma chave de campo é considerada sensível.
     */
    public boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase().replaceAll("[_-]", "");
        return SENSITIVE_KEYS.stream()
                .map(k -> k.replaceAll("[_-]", ""))
                .anyMatch(lower::contains);
    }
}
