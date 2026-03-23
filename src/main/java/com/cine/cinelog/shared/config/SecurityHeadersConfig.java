package com.cine.cinelog.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que adiciona security headers HTTP em todas as respostas.
 *
 * <h3>A05 (OWASP) — Security Misconfiguration</h3>
 * <p>
 * Sem security headers, o browser usa comportamentos padrão que frequentemente
 * são inseguros (retrocompatibilidade com sites antigos). Este filtro instrui o
 * browser a ativar proteções que <b>mitigam XSS, clickjacking, MIME sniffing,
 * downgrade HTTPS→HTTP e fingerprinting</b>.
 * </p>
 *
 * <h3>Por que um filtro separado e não apenas
 * {@code HttpSecurity.headers()}?</h3>
 * <ul>
 * <li>{@code HttpSecurity.headers()} cobre HSTS, X-Frame-Options e
 * X-Content-Type-Options</li>
 * <li>Mas NÃO inclui {@code Permissions-Policy}, {@code Referrer-Policy},
 * {@code Content-Security-Policy} completo, nem remoção de
 * {@code Server}/{@code X-Powered-By}</li>
 * <li>Este filtro complementa o Spring Security com headers avançados</li>
 * </ul>
 *
 * <h3>Cenário de ataque prevenido</h3>
 * <p>
 * Sem {@code X-Content-Type-Options: nosniff}, um atacante pode fazer upload de
 * um
 * arquivo {@code .jpg} que é HTML com JavaScript. O browser "adivinha" (MIME
 * sniffing)
 * que é HTML e <b>executa o script</b> → XSS.
 * </p>
 *
 * @since 1.1
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersConfig extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // ── Content-Security-Policy ──
        // Define quais recursos (scripts, estilos, imagens) o browser pode carregar.
        // "default-src 'self'" = só carrega recursos do próprio domínio.
        // Previne XSS: mesmo que atacante injete <script src="evil.com/malware.js">,
        // o browser recusa carregar porque evil.com não está na whitelist.
        String uri = request.getRequestURI();
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            // Swagger UI precisa de 'unsafe-inline' para scripts e connect-src
            // para o token endpoint do Keycloak (OAuth2 PKCE flow)
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; "
                            + "script-src 'self' 'unsafe-inline'; "
                            + "style-src 'self' 'unsafe-inline'; "
                            + "img-src 'self' data:; "
                            + "font-src 'self'; "
                            + "connect-src 'self' http://localhost:8180; "
                            + "frame-ancestors 'none'; "
                            + "base-uri 'self'; "
                            + "form-action 'self'");
        } else {
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; "
                            + "script-src 'self'; "
                            + "style-src 'self' 'unsafe-inline'; "
                            + "img-src 'self' data:; "
                            + "font-src 'self'; "
                            + "connect-src 'self'; "
                            + "frame-ancestors 'none'; "
                            + "base-uri 'self'; "
                            + "form-action 'self'");
        }

        // ── Referrer-Policy ──
        // Controla quanta informação da URL anterior o browser envia no header Referer.
        // "strict-origin-when-cross-origin":
        // - Mesmo site: envia URL completa
        // - Outro site (HTTPS→HTTPS): envia só a origem (domínio)
        // - HTTPS→HTTP: não envia nada
        // Previne vazamento de dados sensíveis em URLs (ex: tokens em query strings).
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // ── Permissions-Policy (antigo Feature-Policy) ──
        // Controla quais APIs do browser a página pode usar.
        // Desabilita câmera, microfone, geolocalização e payment (desnecessárias para
        // CineLog).
        // Previne: site comprometido ativando câmera/microfone silenciosamente.
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // ── Remoção de headers que expõem tecnologia (fingerprinting) ──
        // Atacantes usam esses headers para identificar a stack e buscar CVEs
        // específicas.
        // Ex: "Server: Apache-Coyote/1.1" → atacante busca CVEs para Tomcat.
        response.setHeader("Server", "");
        response.setHeader("X-Powered-By", "");

        // ── Cache-Control para dados sensíveis ──
        // Para respostas de API, impede que proxies/browsers armazenem cache de dados
        // pessoais.
        // Sem isso, dados do usuário podem ficar no cache do browser mesmo após logout.
        if (uri.startsWith("/api/") && !uri.startsWith("/api/v1/auth/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}
