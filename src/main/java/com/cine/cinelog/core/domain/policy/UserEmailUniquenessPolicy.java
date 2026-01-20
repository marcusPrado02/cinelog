package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.User;
/**
 * Política de domínio para gerenciamento de useremailuniqueness.
 * Define as regras e validações relacionadas a useremailuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em UserEmailUniqueness.</p>
 * 
 * @since 1.0
 * @see UserEmailUniqueness
 */

public interface UserEmailUniquenessPolicy {
    void validateCreate(User user);

    void validateUpdate(Long id, User updated);
}