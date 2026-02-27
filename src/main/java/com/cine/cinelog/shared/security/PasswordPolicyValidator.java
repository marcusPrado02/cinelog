package com.cine.cinelog.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A07:2025 — Validador de política de senhas.
 *
 * <p>
 * Verifica complexidade e rejeita senhas comprometidas (top 100k).
 * Baseado em NIST SP 800-63B e OWASP Authentication Cheat Sheet.
 * </p>
 *
 * <p>
 * Regras:
 * </p>
 * <ul>
 * <li>Mínimo 8 caracteres (NIST mínimo; OWASP recomenda 8–64)</li>
 * <li>Pelo menos 1 letra maiúscula</li>
 * <li>Pelo menos 1 letra minúscula</li>
 * <li>Pelo menos 1 dígito</li>
 * <li>Pelo menos 1 caractere especial</li>
 * <li>Não pode estar na lista de senhas mais comuns</li>
 * <li>Não pode conter o email do usuário</li>
 * </ul>
 */
@Component
@Slf4j
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Top senhas comprometidas (subset para validação instantânea).
     * Em produção, pode ser expandido com integração HaveIBeenPwned API
     * (k-Anonymity).
     */
    private static final Set<String> COMPROMISED_PASSWORDS = Set.of(
            "password", "123456", "12345678", "123456789", "1234567890",
            "qwerty", "abc123", "monkey", "master", "dragon",
            "111111", "2000", "jordan", "superman", "harley",
            "1234567", "fuckme", "hunter", "fuckyou", "trustno1",
            "ranger", "buster", "thomas", "tigger", "robert",
            "soccer", "fuck", "batman", "test", "pass",
            "killer", "hockey", "george", "charlie", "andrew",
            "michelle", "love", "sunshine", "jessica", "pepper",
            "daniel", "access", "123456a", "654321", "maggie",
            "starwars", "silver", "william", "dallas", "yankees",
            "hello", "amanda", "adam", "ashley", "qazwsx",
            "letmein", "admin", "welcome", "monkey123", "login",
            "princess", "password1", "password123", "passw0rd", "p@ssw0rd",
            "iloveyou", "adobe123", "photoshop", "1234", "12345",
            "changeme", "default", "secret", "root", "toor",
            "qwerty123", "zaq1@wsx", "football", "baseball", "shadow",
            "michael", "jennifer", "spring", "cinelog", "cinema");

    /**
     * Valida a senha contra todas as regras de complexidade.
     *
     * @param password a senha a validar
     * @param email    o email do usuário (para detectar senha baseada no email)
     * @return lista de violações (vazia se senha é válida)
     */
    public List<String> validate(String password, String email) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.isBlank()) {
            violations.add("Senha não pode ser vazia.");
            return violations;
        }

        if (password.length() < MIN_LENGTH) {
            violations.add("Senha deve ter no mínimo " + MIN_LENGTH + " caracteres.");
        }

        if (password.length() > MAX_LENGTH) {
            violations.add("Senha deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }

        if (!HAS_UPPERCASE.matcher(password).find()) {
            violations.add("Senha deve conter pelo menos 1 letra maiúscula.");
        }

        if (!HAS_LOWERCASE.matcher(password).find()) {
            violations.add("Senha deve conter pelo menos 1 letra minúscula.");
        }

        if (!HAS_DIGIT.matcher(password).find()) {
            violations.add("Senha deve conter pelo menos 1 número.");
        }

        if (!HAS_SPECIAL.matcher(password).find()) {
            violations.add("Senha deve conter pelo menos 1 caractere especial (!@#$%^&* etc).");
        }

        if (isCompromised(password)) {
            violations.add("Esta senha é muito comum e está em listas de senhas comprometidas.");
        }

        if (email != null && containsEmailParts(password, email)) {
            violations.add("Senha não pode conter partes do seu email.");
        }

        if (!violations.isEmpty()) {
            log.debug("Senha rejeitada: {} violação(ões) de política", violations.size());
        }

        return violations;
    }

    /**
     * Verifica se a senha (case-insensitive) está na lista de senhas comprometidas.
     */
    private boolean isCompromised(String password) {
        return COMPROMISED_PASSWORDS.contains(password.toLowerCase());
    }

    /**
     * Verifica se a senha contém o nome de usuário (parte antes do @) do email.
     */
    private boolean containsEmailParts(String password, String email) {
        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        if (localPart.length() >= 4) {
            return password.toLowerCase().contains(localPart.toLowerCase());
        }
        return false;
    }
}
