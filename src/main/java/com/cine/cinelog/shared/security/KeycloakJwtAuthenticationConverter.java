package com.cine.cinelog.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Conversor de roles Keycloak → {@link GrantedAuthority} do Spring Security.
 *
 * <p>
 * O Keycloak publica papéis no claim {@code realm_access.roles} (realm roles)
 * e em {@code resource_access.<client-id>.roles} (client roles). Este conversor
 * extrai ambos e os expõe com o prefixo {@code ROLE_} para compatibilidade com
 * as anotações {@code @PreAuthorize("hasRole('ADMIN')")} já existentes.
 * </p>
 *
 * <p>
 * <strong>Exemplo de payload Keycloak:</strong>
 * 
 * <pre>
 * {
 *   "iss": "http://localhost:8180/realms/cinelog",
 *   "sub": "uuid-do-usuario",
 *   "preferred_username": "alice@example.com",
 *   "realm_access": { "roles": ["USER", "ADMIN"] },
 *   "resource_access": {
 *     "cinelog-app": { "roles": ["catalog:read"] }
 *   }
 * }
 * </pre>
 * </p>
 *
 * @since 2.0 (IAM – Semana 2)
 */
@Component
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    /**
     * Cliente Keycloak cujas roles de recurso também devem ser importadas.
     * Configurável via propriedade {@code cinelog.security.keycloak.client-id}.
     */
    private String clientId = "cinelog-app";

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /** Conversor padrão para escopos OAuth2 (scope → SCOPE_xxx). */
    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                scopeConverter.convert(jwt).stream(),
                extractKeycloakRoles(jwt).stream()).collect(Collectors.toList());

        // Usa preferred_username como principal name (padrão Keycloak)
        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Extrai roles do Keycloak e as converte para {@code ROLE_<ROLE>}.
     */
    private List<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        return Stream.concat(
                extractRealmRoles(jwt).stream(),
                extractClientRoles(jwt).stream()).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null)
            return List.of();

        List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);
        if (roles == null)
            return List.of();

        return roles.stream()
                .filter(r -> !r.startsWith("default-roles-") // filtrar roles internas
                        && !r.equals("offline_access")
                        && !r.equals("uma_authorization"))
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null)
            return List.of();

        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        if (clientAccess == null)
            return List.of();

        List<String> roles = (List<String>) clientAccess.get(ROLES_CLAIM);
        if (roles == null)
            return List.of();

        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase().replace(":", "_")))
                .collect(Collectors.toList());
    }
}
