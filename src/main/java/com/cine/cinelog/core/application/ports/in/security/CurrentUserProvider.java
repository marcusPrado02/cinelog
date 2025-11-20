package com.cine.cinelog.core.application.ports.in.security;

import java.util.Optional;

import com.cine.cinelog.shared.security.AuthenticatedUser;

public interface CurrentUserProvider {

    Optional<AuthenticatedUser> getCurrentUser();

    default AuthenticatedUser getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));
    }
}