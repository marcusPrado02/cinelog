package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserPolicy;

/**
 * Regras U1–U2, compatíveis com o modelo atual de User:
 *
 * U1: nome obrigatório, trim e tamanho máximo.
 * U2: e-mail obrigatório, trim, formato válido.
 */
@Component
public class DefaultUserPolicy implements UserPolicy {

    // Alinhado com a coluna da tabela users (length = 120)
    private static final int MAX_NAME_LENGTH = 120;

    // Alinhado com o length = 255 da coluna email
    private static final int MAX_EMAIL_LENGTH = 255;

    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void validateCreate(User user) {
        common(user);
    }

    @Override
    public void validateUpdate(User user) {
        common(user);
    }

    private void common(User user) {
        if (user == null) {
            throw DomainException.of(ErrorCode.USER_INVALID);
        }

        // U1: nome obrigatório
        if (user.getName() == null || user.getName().isBlank()) {
            throw DomainException.of(ErrorCode.USER_NAME_INVALID, "name is required");
        }

        String normalizedName = user.getName().trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw DomainException.of(
                    ErrorCode.USER_NAME_INVALID,
                    "name too long",
                    MAX_NAME_LENGTH,
                    normalizedName.length());
        }
        user.setName(normalizedName);

        // U2: e-mail obrigatório + formato válido
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw DomainException.of(ErrorCode.USER_EMAIL_INVALID, "email is required");
        }

        String normalizedEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > MAX_EMAIL_LENGTH
                || !EMAIL_REGEX.matcher(normalizedEmail).matches()) {
            throw DomainException.of(ErrorCode.USER_EMAIL_INVALID, normalizedEmail);
        }
        user.setEmail(normalizedEmail);
    }
}
