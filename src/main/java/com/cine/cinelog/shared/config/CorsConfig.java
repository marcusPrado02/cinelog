package com.cine.cinelog.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração centralizada de CORS (Cross-Origin Resource Sharing).
 *
 * <h3>A05 (OWASP) — Security Misconfiguration</h3>
 *
 * <h3>O que é CORS?</h3>
 * <p>
 * CORS é um mecanismo de segurança do browser que <b>bloqueia</b> por padrão
 * requisições JavaScript de uma origem (domínio) para outra diferente.
 * </p>
 *
 * <p>
 * <b>Exemplo:</b> frontend em {@code https://cinelog.com} chama API em
 * {@code https://api.cinelog.com}. O browser vê que as origens diferem e
 * <b>bloqueia</b> a resposta — a menos que a API retorne headers CORS.
 * </p>
 *
 * <h3>O que é uma "origem"?</h3>
 * <p>
 * Combinação de <b>protocolo + domínio + porta</b>:
 * </p>
 * <ul>
 * <li>{@code https://cinelog.com} ≠ {@code https://api.cinelog.com}
 * (domínio)</li>
 * <li>{@code http://localhost:3000} ≠ {@code http://localhost:8080}
 * (porta)</li>
 * <li>{@code https://cinelog.com/api} = {@code https://cinelog.com/app} (mesma
 * origem)</li>
 * </ul>
 *
 * <h3>Cenário de ataque sem CORS restritivo</h3>
 * <p>
 * Se a API aceita qualquer origem ({@code *}):
 * </p>
 * <ol>
 * <li>Atacante cria site {@code evil.com}</li>
 * <li>Inclui JS que faz fetch para {@code api.cinelog.com}</li>
 * <li>Usuário logado visita {@code evil.com}, browser envia JWT
 * automaticamente</li>
 * <li>Atacante lê a resposta (dados pessoais, reviews, etc.)</li>
 * </ol>
 *
 * <h3>Por que configurar por profile?</h3>
 * <ul>
 * <li><b>Dev:</b> permite {@code localhost:3000/5173/4200}
 * (React/Vue/Angular)</li>
 * <li><b>Prod:</b> só permite a origem real definida via env var</li>
 * </ul>
 *
 * @since 1.1
 */
@Configuration
public class CorsConfig {

    @Value("${cinelog.security.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Value("${cinelog.security.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${cinelog.security.cors.max-age-seconds:3600}")
    private long maxAgeSeconds;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origens permitidas — NUNCA usar "*" com credentials
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // Métodos HTTP permitidos
        config.setAllowedMethods(Arrays.asList(allowedMethods));

        // Headers que o cliente pode enviar
        config.setAllowedHeaders(List.of(
                "Authorization", // JWT Bearer token
                "Content-Type", // application/json
                "Accept", // negociação de conteúdo
                "X-Request-ID" // rastreamento
        ));

        // Headers que o cliente pode ler da resposta
        config.setExposedHeaders(List.of(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset",
                "X-Request-ID"));

        // Permite envio de credentials (cookies, Authorization header)
        config.setAllowCredentials(true);

        // Tempo que o browser cacheia a resposta do preflight (OPTIONS)
        config.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}
