package com.cine.cinelog.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuração OpenAPI/Swagger com suporte dual de autenticação:
 * <ol>
 * <li><strong>BearerAuth</strong> — JWT local via {@code /api/auth/login}.</li>
 * <li><strong>keycloak-sso</strong> — Authorization Code Flow + PKCE
 * (SSO).</li>
 * </ol>
 *
 * @since 1.0 / evoluído em 2.0 (IAM – Semana 2)
 */
@Configuration
@Profile({ "dev", "test", "default" })
public class OpenApiConfig {

        @Value("${cinelog.security.keycloak.issuer-uri:http://localhost:8180/realms/cinelog}")
        private String keycloakIssuerUri;

        @Bean
        public OpenAPI cinelogOpenAPI() {
                var info = new Info()
                                .title("CineLog API")
                                .description("""
                                                Backend para registrar e recomendar filmes/séries.

                                                **Autenticação disponível:**
                                                - **BearerAuth** — JWT local via `POST /api/auth/login`
                                                - **keycloak-sso** — OAuth2 Authorization Code Flow (SSO via Keycloak + MFA)

                                                Para SSO: clique em *Authorize*, selecione `keycloak-sso` e faça login.
                                                """)
                                .version("v1")
                                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                                .contact(new Contact().name("Marcus Prado").email("silvamarcusprado@gmail.com"));

                // Esquema 1: JWT local (compatibilidade com fluxo existente)
                var jwtScheme = new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido via POST /api/auth/login");

                // Esquema 2: OAuth2 Keycloak — Authorization Code + PKCE (SSO)
                String authUrl = keycloakIssuerUri + "/protocol/openid-connect/auth";
                String tokenUrl = keycloakIssuerUri + "/protocol/openid-connect/token";

                var keycloakScopes = new Scopes()
                                .addString("openid", "OpenID Connect identity")
                                .addString("profile", "Perfil do usuário")
                                .addString("email", "Endereço de e-mail");

                var authCodeFlow = new OAuthFlow()
                                .authorizationUrl(authUrl)
                                .tokenUrl(tokenUrl)
                                .scopes(keycloakScopes);

                var keycloakScheme = new SecurityScheme()
                                .name("keycloak-sso")
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("""
                                                SSO via Keycloak (Authorization Code + PKCE).
                                                MFA habilitado — Keycloak solicitará OTP quando configurado para o usuário.
                                                Roles extraídas automaticamente do claim `realm_access.roles`.
                                                """)
                                .flows(new OAuthFlows().authorizationCode(authCodeFlow));

                return new OpenAPI()
                                .info(info)
                                .servers(List.of(
                                                new Server().url("http://localhost:8080").description("Dev")))
                                .components(new Components()
                                                .addSecuritySchemes("BearerAuth", jwtScheme)
                                                .addSecuritySchemes("keycloak-sso", keycloakScheme))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("BearerAuth")
                                                .addList("keycloak-sso"));
        }

        /**
         * Customiza a documentação OpenAPI para parâmetros do tipo Pageable.
         */
        @Bean
        public OperationCustomizer swaggerPageableCustomizer() {
                return (operation, handlerMethod) -> {
                        boolean hasPageable = Arrays.stream(handlerMethod.getMethodParameters())
                                        .anyMatch(param -> Pageable.class.isAssignableFrom(param.getParameterType()));

                        if (hasPageable && operation.getParameters() != null) {
                                List<Parameter> params = operation.getParameters().stream()
                                                .filter(p -> !p.getName().toLowerCase().contains("sort"))
                                                .collect(Collectors.toList());

                                StringSchema sortSchema = new StringSchema();
                                sortSchema.setExample("id,asc");
                                sortSchema.setDefault("id");
                                sortSchema.setPattern("^[a-zA-Z0-9_]+(,(asc|desc))?$");

                                Parameter sortParam = new Parameter()
                                                .name("sort")
                                                .in("query")
                                                .required(false)
                                                .description(
                                                                "Campo de ordenação (ex: 'id,asc', 'name,desc'). Formato: propriedade ou propriedade,direção")
                                                .schema(sortSchema);

                                params.add(sortParam);
                                operation.setParameters(params);
                        }

                        return operation;
                };
        }
}
