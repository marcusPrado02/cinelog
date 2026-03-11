package com.cine.cinelog.shared.config;

import com.cine.cinelog.shared.security.KeycloakJwtAuthenticationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração OAuth2 / Keycloak — <strong>IAM Semana 2</strong>.
 *
 * <p>
 * Condicionado à propriedade
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. Quando não
 * configurado (ex: testes unitários, ambientes sem Keycloak), este bean
 * <em>não é criado</em> e o sistema opera somente com autenticação JWT local.
 * </p>
 *
 * <p>
 * O {@link org.springframework.security.oauth2.jwt.JwtDecoder} é
 * auto-configurado pelo Spring Boot via
 * {@code OAuth2ResourceServerJwtAutoConfiguration} (usa JWKS do issuer-uri).
 * Não criamos nosso próprio decoder para evitar conflito com
 * {@code @ConditionalOnMissingBean(JwtDecoder.class)}.
 * </p>
 *
 * <h2>Fluxo de autenticação dual</h2>
 * <ol>
 * <li>{@code JwtAuthenticationFilter} tenta validar o Bearer como token
 * local (HMAC). Se o claim {@code iss} aponta para Keycloak, ignora.</li>
 * <li>{@code BearerTokenAuthenticationFilter} (Spring OAuth2 RS) valida
 * via JWKS e converte roles usando
 * {@link KeycloakJwtAuthenticationConverter}.</li>
 * </ol>
 *
 * @since 2.0 (IAM – Semana 2)
 */
@Configuration
@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")
public class OAuth2SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SecurityConfig.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${cinelog.security.keycloak.client-id:cinelog-app}")
    private String keycloakClientId;

    /**
     * Conversor de JWT Keycloak →
     * {@link org.springframework.security.core.GrantedAuthority}.
     *
     * <p>
     * Extrai {@code realm_access.roles} e
     * {@code resource_access.{clientId}.roles} e os converte em
     * {@code ROLE_XXX} para compatibilidade com {@code @PreAuthorize}.
     * </p>
     */
    @Bean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();
        converter.setClientId(keycloakClientId);
        log.info("IAM: KeycloakJwtAuthenticationConverter configurado (issuer={}, clientId={})",
                issuerUri, keycloakClientId);
        return converter;
    }
}
