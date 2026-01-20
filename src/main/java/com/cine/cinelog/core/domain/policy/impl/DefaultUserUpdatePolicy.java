package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserUpdatePolicy;

/**
 * Regra U4 (versão compatível com o modelo atual de User):
 *
 * U4: e-mail é imutável – não pode ser alterado em updates.
 */
@Component
public class DefaultUserUpdatePolicy implements UserUpdatePolicy {

    @Override
    public void validate(User current, User updated) {
        if (current == null || updated == null) {
            return;
        }

        String currentEmail = current.getEmail();
        String updatedEmail = updated.getEmail();

        // Se mudar o email, barramos
        if (updatedEmail != null && currentEmail != null
                && !currentEmail.equalsIgnoreCase(updatedEmail)) {
            throw DomainException.of(
                    ErrorCode.USER_EMAIL_IMMUTABLE,
                    "email cannot be changed");
        }
    }
}
