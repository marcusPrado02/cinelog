package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.policy.UserEmailUniquenessPolicy;
import com.cine.cinelog.core.domain.model.User;
/**
 * Política de domínio para gerenciamento de defaultuseremailuniqueness.
 * Define as regras e validações relacionadas a defaultuseremailuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultUserEmailUniqueness.</p>
 * 
 * @since 1.0
 * @see DefaultUserEmailUniqueness
 */

@Component
public class DefaultUserEmailUniquenessPolicy implements UserEmailUniquenessPolicy {

    private final UserRepositoryPort repo;

    public DefaultUserEmailUniquenessPolicy(UserRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void validateCreate(User user) {
        if (repo.existsByEmail(user.getEmail())) {
            throw DomainException.of(ErrorCode.USER_EMAIL_DUPLICATE, user.getEmail());
        }
    }

    @Override
    public void validateUpdate(Long id, User updated) {
        if (repo.existsByEmailAndIdNot(updated.getEmail(), id)) {
            throw DomainException.of(ErrorCode.USER_EMAIL_DUPLICATE, updated.getEmail());
        }
    }
}
