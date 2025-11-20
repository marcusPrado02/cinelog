package com.cine.cinelog.shared.security;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<AuthenticatedUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
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
