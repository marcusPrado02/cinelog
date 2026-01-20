package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.User;

/**
 * Regras específicas de update para User.
 *
 * U4: e-mail não pode ser alterado após verificação.
 */
public interface UserUpdatePolicy {

    void validate(User current, User updated);
}