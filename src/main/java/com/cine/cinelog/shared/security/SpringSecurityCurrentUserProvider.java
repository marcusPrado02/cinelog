package com.cine.cinelog.shared.security;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classe de configuração Spring para gerenciamento de
 * springsecuritycurrentuserprovider.
 *
 * <p>
 * Suporta dois fluxos de autenticação:
 * </p>
 * <ul>
 * <li><b>JWT local (HMAC)</b>: principal é {@link CinelogUserDetails}.</li>
 * <li><b>Keycloak OAuth2 (RS256)</b>: principal é {@link Jwt}; o usuário
 * local é localizado pelo e-mail presente nos claims.</li>
 * </ul>
 *
 * @since 1.0 / evoluído em 2.0 (IAM – Semana 2)
 */
@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    /**
     * Usado para resolver o usuário local quando o token é Keycloak (Jwt).
     * {@code @Lazy} evita dependência circular no contexto de segurança.
     */
    @Lazy
    @Autowired
    private UserDetailsService userDetailsService;

    @Lazy
    @Autowired
    private ObjectProvider<WatchEntryRepositoryPort> watchEntryRepositoryProvider;

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

    /**
     * Verifica se o usuario autenticado e dono de um WatchEntry pelo seu ID.
     */
    public boolean ownsWatchEntry(Long watchEntryId) {
        if (watchEntryId == null) return false;
        WatchEntryRepositoryPort repo = watchEntryRepositoryProvider.getIfAvailable();
        if (repo == null) return false;
        Optional<AuthenticatedUser> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) return false;
        Long currentUserId = currentUser.get().id();
        return repo.findById(watchEntryId)
                .map(entry -> currentUserId.equals(entry.getUserId()))
                .orElse(false);
    }

    @Override
    public Optional<AuthenticatedUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        // ── Fluxo 1: JWT local (HMAC) ────────────────────────────────────────
        if (principal instanceof CinelogUserDetails userDetails) {
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority) // "ROLE_USER", "ROLE_ADMIN"
                    .map(role -> role.replace("ROLE_", "")) // normaliza para "USER", "ADMIN"
                    .collect(Collectors.toSet());

            return Optional.of(new AuthenticatedUser(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    roles));
        }

        // ── Fluxo 2: Keycloak OAuth2 (RS256) — principal é Jwt ───────────────
        if (principal instanceof Jwt jwt) {
            // Keycloak coloca o e-mail no claim "email"; fallback para "preferred_username"
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("preferred_username");
            }
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }

            try {
                UserDetails ud = userDetailsService.loadUserByUsername(email);
                if (ud instanceof CinelogUserDetails cud) {
                    // Usa as authorities já extraídas pelo KeycloakJwtAuthenticationConverter
                    Set<String> roles = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(r -> r.startsWith("ROLE_"))
                            .map(r -> r.replace("ROLE_", ""))
                            .collect(Collectors.toSet());
                    return Optional.of(new AuthenticatedUser(
                            cud.getUserId(),
                            email,
                            roles));
                }
            } catch (Exception e) {
                // Usuário não encontrado no banco local — não autenticado
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
