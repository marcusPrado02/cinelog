package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.observability.security.SecurityEvent;
import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Filtro de autenticação JWT que valida tokens Bearer em cada requisição.
 *
 * <p>
 * A09:2025 — Versão corrigida: o catch block agora registra eventos
 * de segurança (antes era silencioso, violando logging de segurança).
 * </p>
 *
 * @since 1.0
 */

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;
    private final SecurityEventLogger securityEventLogger;
    private final SecurityMetricsService securityMetrics;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
            UserDetailsService userDetailsService,
            SecurityEventLogger securityEventLogger,
            SecurityMetricsService securityMetrics) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.securityEventLogger = securityEventLogger;
        this.securityMetrics = securityMetrics;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

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
}
