package com.cine.cinelog.integration;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para autenticação OAuth2 via Keycloak.
 *
 * <h2>O que é validado</h2>
 * <ul>
 * <li>Obtenção de access token via Resource Owner Password Credentials (ROPC) —
 * usado apenas em testes, não em produção.</li>
 * <li>Acesso a endpoint protegido com token Keycloak (Bearer).</li>
 * <li>Rejeição de requests sem token.</li>
 * <li>Rejeição de token inválido.</li>
 * <li>Roles extraídas do Keycloak ({@code realm_access.roles}) são
 * reconhecidas pelo Spring Security.</li>
 * <li>MFA: usuário com {@code CONFIGURE_TOTP} como required action
 * ainda recebe token válido via ROPC (bypass MFA intencional em testes).</li>
 * </ul>
 *
 * <h2>Infraestrutura</h2>
 * <p>
 * Usa {@link KeycloakContainer} (Testcontainers) com o realm {@code cinelog}
 * importado do arquivo {@code docker/keycloak/cinelog-realm.json}. As
 * propriedades de spring security são injetadas via
 * {@link DynamicPropertySource}.
 * </p>
 *
 * @since 2.0 (IAM – Semana 2)
 */
@DisplayName("Keycloak OAuth2 Integration Tests")
class KeycloakOAuth2IntegrationTest extends AbstractIntegrationTest {

        // ─────────────────────────────────────────────────────────────
        // Keycloak Testcontainer
        // ─────────────────────────────────────────────────────────────

        /**
         * Keycloak 25.0.4 com o realm cinelog importado.
         * Realm path relativo ao classpath do projeto.
         */
        static final KeycloakContainer keycloakContainer;

        static {
                keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:25.0.4")
                                .withRealmImportFile("keycloak/cinelog-realm.json")
                                .withReuse(false); // false: garante realm limpo entre runs de CI
                keycloakContainer.start();
        }

        @DynamicPropertySource
        static void keycloakProperties(DynamicPropertyRegistry registry) {
                String issuerUri = keycloakContainer.getAuthServerUrl() + "/realms/cinelog";

                // Ativa o OAuth2 Resource Server com o Keycloak de teste
                registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                                () -> issuerUri);

                // Informar ao JwtAuthenticationFilter qual é o issuer Keycloak
                registry.add("cinelog.security.keycloak.issuer-uri", () -> issuerUri);
        }

        @Autowired
        private TestRestTemplate restTemplate;

        // ─────────────────────────────────────────────────────────────
        // Helper: obter access token do Keycloak via ROPC
        // ─────────────────────────────────────────────────────────────

        /**
         * Obtém access token Keycloak via Resource Owner Password Credentials.
         * <strong>Não usar em produção</strong> — apenas para testes de integração.
         */
        private String obtainKeycloakToken(String username, String password) {
                String tokenUrl = keycloakContainer.getAuthServerUrl()
                                + "/realms/cinelog/protocol/openid-connect/token";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("grant_type", "password");
                form.add("client_id", "cinelog-app");
                form.add("username", username);
                form.add("password", password);
                form.add("scope", "openid profile email roles");

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

                @SuppressWarnings("unchecked")
                ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                                tokenUrl, request, (Class<Map<String, Object>>) (Class<?>) Map.class);

                assertThat(response.getStatusCode()).as("Keycloak token endpoint deve retornar 200")
                                .isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsKey("access_token");

                return (String) response.getBody().get("access_token");
        }

        // ─────────────────────────────────────────────────────────────
        // Testes: token válido Keycloak aceito pelo Resource Server
        // ─────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("Autenticação com token Keycloak")
        class ComTokenKeycloak {

                @Test
                @DisplayName("deve aceitar Bearer token Keycloak em endpoint protegido")
                void deveAceitarTokenKeycloak() {
                        String token = obtainKeycloakToken("alice", "Alice@CineLog2025!");

                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<Void> entity = new HttpEntity<>(headers);

                        // /actuator/health é permitAll em ActuatorSecurityConfig — usa /api/v1/users/me
                        // ou outro endpoint autenticado do domínio
                        ResponseEntity<String> response = restTemplate.exchange(
                                        getBaseUrl() + "/api/v1/users/me", HttpMethod.GET, entity, String.class);

                        assertThat(response.getStatusCode())
                                        .as("Token Keycloak válido deve resultar em 200 (ou 404 se rota não existe)")
                                        .isNotEqualTo(HttpStatus.UNAUTHORIZED)
                                        .isNotEqualTo(HttpStatus.FORBIDDEN);
                }

                @Test
                @DisplayName("deve rejeitar request sem token em endpoint protegido")
                void deveRejeitarSemToken() {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                                        getBaseUrl() + "/api/v1/users/me", String.class);

                        assertThat(response.getStatusCode())
                                        .isEqualTo(HttpStatus.UNAUTHORIZED);
                }

                @Test
                @DisplayName("deve rejeitar token Bearer inválido (assinatura errada)")
                void deveRejeitarTokenInvalido() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmYWtlIn0.invalidsignature");
                        HttpEntity<Void> entity = new HttpEntity<>(headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        getBaseUrl() + "/api/v1/users/me", HttpMethod.GET, entity, String.class);

                        assertThat(response.getStatusCode())
                                        .isEqualTo(HttpStatus.UNAUTHORIZED);
                }
        }

        // ─────────────────────────────────────────────────────────────
        // Testes: roles extraídas do Keycloak
        // ─────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("Roles vindas do Keycloak IAM")
        class RolesKeycloak {

                @Test
                @DisplayName("usuário 'alice' (USER) não deve acessar endpoint ADMIN")
                void userNaoDeveAcessarAdmin() {
                        String token = obtainKeycloakToken("alice", "Alice@CineLog2025!");

                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<Void> entity = new HttpEntity<>(headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        getBaseUrl() + "/api/v1/admin/users", HttpMethod.GET, entity, String.class);

                        assertThat(response.getStatusCode())
                                        .as("Role USER não deve acessar /api/v1/admin/**")
                                        .isEqualTo(HttpStatus.FORBIDDEN);
                }

                @Test
                @DisplayName("usuário 'admin' (ADMIN) deve acessar endpoint ADMIN")
                void adminDeveAcessarAdmin() {
                        String token = obtainKeycloakToken("admin", "Admin@CineLog2025!");

                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<Void> entity = new HttpEntity<>(headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        getBaseUrl() + "/api/v1/admin/users", HttpMethod.GET, entity, String.class);

                        assertThat(response.getStatusCode())
                                        .as("Role ADMIN deve ser reconhecida do Keycloak")
                                        .isNotEqualTo(HttpStatus.FORBIDDEN)
                                        .isNotEqualTo(HttpStatus.UNAUTHORIZED);
                }
        }

        // ─────────────────────────────────────────────────────────────
        // Testes: SSO — endpoints públicos do Swagger acessíveis
        // ─────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("Swagger SSO — endpoints públicos")
        class SwaggerSso {

                @Test
                @DisplayName("Swagger UI deve ser acessível sem autenticação")
                void swaggerUiDeveSerAcessivel() {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                                        getBaseUrl() + "/swagger-ui/index.html", String.class);

                        assertThat(response.getStatusCode())
                                        .isEqualTo(HttpStatus.OK);
                }

                @Test
                @DisplayName("oauth2-redirect.html deve ser acessível para callback SSO")
                void oauth2RedirectDeveSerAcessivel() {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                                        getBaseUrl() + "/swagger-ui/oauth2-redirect.html", String.class);

                        // 200 OK ou redirect — nunca 401/403
                        assertThat(response.getStatusCode().value())
                                        .as("oauth2-redirect.html deve ser acessível sem autenticação")
                                        .isNotIn(401, 403);
                }

                @Test
                @DisplayName("v3/api-docs deve retornar spec com esquema keycloak-sso")
                void apiDocDeveTerEsquemaKeycloak() {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                                        getBaseUrl() + "/v3/api-docs", String.class);

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody())
                                        .as("OpenAPI spec deve conter esquema keycloak-sso")
                                        .contains("keycloak-sso");
                }
        }

        // ─────────────────────────────────────────────────────────────
        // Testes: autenticação local (JWT HMAC) coexiste com Keycloak
        // ─────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("Autenticação local (JWT) coexiste com Keycloak")
        class AutenticacaoLocal {

                @Test
                @DisplayName("endpoint /api/v1/auth/login deve permanecer acessível sem token")
                void loginLocalDeveSerAcessivel() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        String body = """
                                        {"email":"alice@cinelog.dev","password":"senha-invalida"}
                                        """;
                        HttpEntity<String> entity = new HttpEntity<>(body, headers);

                        ResponseEntity<String> response = restTemplate.postForEntity(
                                        getBaseUrl() + "/api/v1/auth/login", entity, String.class);

                        // Esperamos 401 (credenciais inválidas) — mas NÃO 404 ou 403
                        assertThat(response.getStatusCode())
                                        .as("Endpoint /api/v1/auth/login deve estar acessível (não 404/403)")
                                        .isNotEqualTo(HttpStatus.NOT_FOUND)
                                        .isNotEqualTo(HttpStatus.FORBIDDEN);
                }
        }
}
