package com.cine.cinelog.core.application.ports.in.security;

import java.util.Optional;

import com.cine.cinelog.shared.security.AuthenticatedUser;
/**
 * Porta de saída para operação de CurrentUserProvider.
 * Define o contrato para operações relacionadas a currentuserprovider.
 * 
 * <p>Esta interface segue o padrão de Arquitetura Hexagonal,
 * isolando a lógica de domínio das implementações de infraestrutura.</p>
 * 
 * @since 1.0
 */

public interface CurrentUserProvider {

    Optional<AuthenticatedUser> getCurrentUser();

    default AuthenticatedUser getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));
    }
}