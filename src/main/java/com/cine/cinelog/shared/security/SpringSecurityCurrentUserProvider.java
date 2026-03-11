package com.cine.cinelog.shared.security;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classe de configuração Spring para gerenciamento de springsecuritycurrentuserprovider.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */
@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    /**
     * Verifica se o usuário autenticado possui o ID informado.
     * Projetado para uso em expressões SpEL de {@code @PreAuthorize}.
     *
     * @param id ID do usuário a comparar
     * @return {@code true} se o usuário logado tem o mesmo ID
     */
    public boolean isCurrentUser(Long id) {
        return getCurrentUser()
                .map(u -> u.id().equals(id))
                .orElse(false);
    }

    @Override
    public Optional<AuthenticatedUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof CinelogUserDetails userDetails)) {
            return Optional.empty();
        }

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // "ROLE_USER", "ROLE_ADMIN"
                .map(role -> role.replace("ROLE_", "")) // normaliza para "USER", "ADMIN"
                .collect(Collectors.toSet());

        AuthenticatedUser current = new AuthenticatedUser(
                userDetails.getUserId(),
                userDetails.getUsername(),
                roles);

        return Optional.of(current);
    }
}
