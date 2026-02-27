package com.cine.cinelog.shared.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança dedicada para endpoints do Actuator.
 *
 * <h3>A05 (OWASP) — Security Misconfiguration</h3>
 *
 * <h3>O que é o Actuator?</h3>
 * <p>
 * Módulo do Spring Boot que expõe endpoints operacionais:
 * </p>
 * <ul>
 * <li>{@code /actuator/health} — status da aplicação (UP/DOWN)</li>
 * <li>{@code /actuator/info} — informações da build</li>
 * <li>{@code /actuator/env} — ⚠️ variáveis de ambiente (pode expor
 * secrets!)</li>
 * <li>{@code /actuator/heapdump} — ⚠️ dump de memória (tokens, senhas!)</li>
 * <li>{@code /actuator/configprops} — ⚠️ propriedades de configuração</li>
 * <li>{@code /actuator/mappings} — ⚠️ mapa completo dos endpoints HTTP</li>
 * </ul>
 *
 * <h3>Cenário de ataque</h3>
 * <p>
 * Se {@code /actuator/**} estiver público:
 * </p>
 * <ol>
 * <li>Atacante acessa {@code /actuator/env} → descobre JWT_SECRET e credenciais
 * DB</li>
 * <li>Acessa {@code /actuator/heapdump} → extrai tokens JWT ativos da
 * memória</li>
 * <li>Com o secret e tokens, acessa a API como qualquer usuário</li>
 * </ol>
 *
 * <h3>Regra aplicada</h3>
 * <ul>
 * <li>{@code /health} e {@code /info} → públicos (load balancer / k8s
 * probes)</li>
 * <li>Demais endpoints → exigem role {@code ADMIN}</li>
 * </ul>
 *
 * <p>
 * O {@code @Order(1)} garante que este SecurityFilterChain é avaliado
 * <b>antes</b> do chain principal, pois o Spring usa a primeira chain cujo
 * {@code securityMatcher} combina com a request.
 * </p>
 *
 * @since 1.1
 */
@Configuration
@Order(1)
public class ActuatorSecurityConfig {

    @Bean
    SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                        .permitAll()
                        .anyRequest().hasRole("ADMIN"));

        return http.build();
    }
}
