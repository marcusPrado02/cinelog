package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.User;

/**
 * U5: não permite deletar usuário com histórico (watch entries / watchlist).
 */
public interface UserDeletionPolicy {

    void validateDelete(User user);
}