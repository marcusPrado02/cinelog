package com.cine.cinelog.shared.security;

import java.util.Set;
/**
 * Classe de configuração Spring para gerenciamento de authenticateduser.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public record AuthenticatedUser(
        Long id,
        String email,
        Set<String> roles) {

    public boolean hasRole(String role) {
        if (roles == null)
            return false;
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}