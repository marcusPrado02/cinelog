package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.observability.security.SecurityEvent;
import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Filtro de autenticação JWT que valida tokens Bearer locais (HMAC/HS256).
 *
 * <p>
 * <strong>Tokens Keycloak (RS256):</strong> detectados inspecionando o claim
 * {@code iss}
 * do payload JWT (sem verificação de assinatura). Quando identificados, o
 * filtro os
 * ignora — a validação é delegada ao {@code BearerTokenAuthenticationFilter}
 * (Spring OAuth2 Resource Server).
 * </p>
 *
 * <p>
 * A09:2025 — registra eventos de segurança em todos os caminhos de erro.
 * </p>
 *
 * @since 1.0 / evoluído em 2.0 (IAM – Semana 2)
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;
    private final SecurityEventLogger securityEventLogger;
    private final SecurityMetricsService securityMetrics;

    /** URI do issuer Keycloak. Nulo quando Keycloak não está configurado. */
    private String keycloakIssuerUri;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
            UserDetailsService userDetailsService,
            SecurityEventLogger securityEventLogger,
            SecurityMetricsService securityMetrics) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.securityEventLogger = securityEventLogger;
        this.securityMetrics = securityMetrics;
    }

    /** Injetado pelo Spring quando a propriedade está configurada. */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
    public void setKeycloakIssuerUri(String keycloakIssuerUri) {
        this.keycloakIssuerUri = keycloakIssuerUri;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // IAM – Semana 2: se o token foi emitido pelo Keycloak (RS256),
            // delegar ao BearerTokenAuthenticationFilter — não tentar validar
            // com a chave HMAC local (causaria SignatureException falso-alarme).
            if (isKeycloakToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String email = jwtTokenService.extractSubject(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userDetails = userDetailsService.loadUserByUsername(email);

                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (ExpiredJwtException e) {
                // A09:2025 — JWT expirado: evento INFO (normal em operação)
                securityEventLogger.log(SecurityEvent.JWT_EXPIRED, Map.of(
                        "uri", InputSanitizer.sanitizeForLog(request.getRequestURI()),
                        "reason", "expired"));
                securityMetrics.incrementJwtFailure("expired");
                SecurityContextHolder.clearContext();
            } catch (MalformedJwtException | SignatureException e) {
                // A09:2025 — JWT adulterado/inválido: evento WARNING (possível ataque)
                securityEventLogger.log(SecurityEvent.JWT_INVALID, Map.of(
                        "uri", InputSanitizer.sanitizeForLog(request.getRequestURI()),
                        "reason", e.getClass().getSimpleName()));
                securityMetrics.incrementJwtFailure("invalid");
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                // A09:2025 — Erro inesperado na validação de JWT
                securityEventLogger.log(SecurityEvent.JWT_INVALID, Map.of(
                        "uri", InputSanitizer.sanitizeForLog(request.getRequestURI()),
                        "reason", e.getClass().getSimpleName()));
                securityMetrics.incrementJwtFailure("unknown");
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    // ─────────────────────────────────────────────────────────────
    // Keycloak token detection (sem verificação de assinatura)
    // ─────────────────────────────────────────────────────────────

    /**
     * Detecta se um Bearer token foi emitido pelo Keycloak inspecionando o claim
     * {@code iss} do payload JWT (decodificado em Base64, sem verificação de
     * assinatura).
     *
     * <p>
     * Retorna {@code false} se {@code keycloakIssuerUri} não está configurado
     * (Keycloak desabilitado) ou se o payload não pode ser lido.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private boolean isKeycloakToken(String token) {
        if (keycloakIssuerUri == null || keycloakIssuerUri.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2)
                return false;

            byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            Map<String, Object> claims = OBJECT_MAPPER.readValue(payloadBytes, Map.class);
            String iss = (String) claims.get("iss");
            return keycloakIssuerUri.equals(iss);
        } catch (Exception e) {
            return false; // payload ilegível → tratar como token local
        }
    }

    /** Adiciona padding Base64 se necessário. */
    private static String padBase64(String base64) {
        int mod = base64.length() % 4;
        if (mod == 2)
            return base64 + "==";
        if (mod == 3)
            return base64 + "=";
        return base64;
    }
}
