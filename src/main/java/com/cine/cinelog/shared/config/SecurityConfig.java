package com.cine.cinelog.shared.config;

import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import com.cine.cinelog.shared.security.JwtAuthenticationFilter;
import com.cine.cinelog.shared.security.JwtTokenService;
import com.cine.cinelog.shared.security.KeycloakJwtAuthenticationConverter;
import com.cine.cinelog.shared.security.RateLimitFilter;
import com.cine.cinelog.shared.security.SqlInjectionFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuração de segurança Spring Security com suporte dual:
 * <ol>
 * <li><strong>JWT local</strong> — via {@link JwtAuthenticationFilter}
 * (HMAC/HS256).</li>
 * <li><strong>Keycloak OAuth2</strong> — via {@code oauth2ResourceServer()} com
 * validação
 * JWKS (RS256). Ativo apenas quando
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} está
 * configurado.</li>
 * </ol>
 *
 * <p>
 * <strong>Fluxo de decisão:</strong>
 * </p>
 * 
 * <pre>
 *   Bearer token recebido
 *   ├─ iss == Keycloak → JwtAuthenticationFilter ignora →
 *   │   BearerTokenAuthenticationFilter valida via JWKS
 *   └─ iss ausente (token local) → JwtAuthenticationFilter valida via HMAC
 * </pre>
 *
 * @since 1.0 / evoluído em 2.0 (IAM – Semana 2)
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        /**
         * Conversor de roles Keycloak → Spring GrantedAuthority.
         * {@code null} quando Keycloak não está configurado (sem issuer-uri).
         */
        @Autowired(required = false)
        private KeycloakJwtAuthenticationConverter keycloakJwtConverter;

        /**
         * JwtDecoder Keycloak (NimbusJwtDecoder via JWKS).
         * {@code null} quando {@code issuer-uri} não está configurado.
         */
        @Autowired(required = false)
        private JwtDecoder jwtDecoder;

        @Bean
        public PasswordEncoder passwordEncoder() {
                // A02: BCrypt com fator de trabalho 12 (mínimo recomendado OWASP)
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(
                        UserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {

                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(
                        JwtTokenService jwtTokenService,
                        UserDetailsService userDetailsService,
                        SecurityEventLogger securityEventLogger,
                        SecurityMetricsService securityMetrics) {
                return new JwtAuthenticationFilter(jwtTokenService, userDetailsService,
                                securityEventLogger, securityMetrics);
        }

        @Bean
        public SqlInjectionFilter sqlInjectionFilter(
                        SecurityEventLogger securityEventLogger,
                        SecurityMetricsService securityMetrics) {
                return new SqlInjectionFilter(securityEventLogger, securityMetrics);
        }

        @Bean
        @Order(2)
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        DaoAuthenticationProvider authProvider,
                        JwtAuthenticationFilter jwtFilter,
                        SqlInjectionFilter sqlInjectionFilter,
                        RateLimitFilter rateLimitFilter,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {

                http
                                // A05: CORS restritivo — origens configuráveis por profile
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authProvider)
                                // A02/A05: Security headers via Spring Security
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.deny())
                                                .contentTypeOptions(cto -> {
                                                })
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)))
                                .authorizeHttpRequests(auth -> auth
                                                // Docs (condicionados por springdoc.enabled via profile)
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                // IAM: redirect OAuth2 do Swagger e callbacks Keycloak
                                                .requestMatchers(
                                                                "/swagger-ui/oauth2-redirect.html",
                                                                "/login/oauth2/code/**",
                                                                "/oauth2/authorization/**")
                                                .permitAll()
                                                .requestMatchers("/api/auth/**")
                                                .permitAll()
                                                // Actuator: regras dedicadas em ActuatorSecurityConfig (@Order 1)
                                                // Endpoints ADMIN
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPS")
                                                // Endpoints gerais (usuário logado)
                                                .anyRequest().authenticated())
                                // A04: Rate limit primeiro (bloqueia DoS antes de qualquer processamento)
                                .addFilterBefore(rateLimitFilter, SqlInjectionFilter.class)
                                // A03: Detecção de SQL injection antes de autenticação
                                .addFilterBefore(sqlInjectionFilter, JwtAuthenticationFilter.class)
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                // IAM – Semana 2: OAuth2 Resource Server (Keycloak).
                // Só ativo quando spring.security.oauth2.resourceserver.jwt.issuer-uri estiver
                // configurado. Valida tokens RS256 via JWKS e extrai roles do Keycloak.
                if (jwtDecoder != null) {
                        http.oauth2ResourceServer(oauth2 -> oauth2
                                        .jwt(jwt -> {
                                                jwt.decoder(jwtDecoder);
                                                if (keycloakJwtConverter != null) {
                                                        jwt.jwtAuthenticationConverter(keycloakJwtConverter);
                                                }
                                        }));
                }

                return http.build();
        }
}
