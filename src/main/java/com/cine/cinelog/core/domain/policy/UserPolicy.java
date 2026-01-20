package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.User;
/**
 * Política de domínio para gerenciamento de user.
 * Define as regras e validações relacionadas a user.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em User.</p>
 * 
 * @since 1.0
 * @see User
 */

public interface UserPolicy {

    void validateCreate(User user);

    void validateUpdate(User user);
}