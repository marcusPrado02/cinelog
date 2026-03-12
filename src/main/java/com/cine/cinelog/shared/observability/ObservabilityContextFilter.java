package com.cine.cinelog.shared.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para popular o MDC com dados de observabilidade (traceId, spanId,
 * userAgent, clientIp, userId, userEmail, etc) para todos os requests.
 * Também gera um log-resumo único por request.
 */
@Component
/**
 * Classe de configuração Spring para gerenciamento de observabilitycontextfilter.
 *
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 *
 * @since 1.0
 */
@Order(1)
public class ObservabilityContextFilter implements Filter {

    /**
     * Popula o MDC e gera log-resumo do request.
     *
     * @param servletRequest
     * @param servletResponse
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        long start = System.currentTimeMillis();

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        // Trace/Span (aceita se cliente enviar, senão cria)
        String traceId = headerOrNew(req, "X-Trace-Id");
        String spanId = headerOrNew(req, "X-Span-Id");

        // User agent & ip
        String userAgent = safe(req.getHeader("User-Agent"));
        String clientIp = safe(firstNonEmpty(
                req.getHeader("X-Forwarded-For"),
                req.getRemoteAddr()));

        // Diag SQL (reservado para uso futuro — ex: ativar log de queries)
        // String diagHeader = req.getHeader("X-Diag-SQL");

        // Preenche MDC (vai para o JSON do Logback)
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("method", req.getMethod());
        MDC.put("path", req.getRequestURI());
        MDC.put("query", safe(req.getQueryString()));
        MDC.put("clientIp", clientIp);
        MDC.put("userAgent", userAgent);

        try {
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            long took = System.currentTimeMillis() - start;
            MDC.put("status", String.valueOf(res.getStatus()));
            MDC.put("tookMs", String.valueOf(took));

            // Enriquece MDC com usuário autenticado (JWT filter já rodou dentro do chain.doFilter)
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                    MDC.put("userId", auth.getName());
                }
            } catch (Exception ignored) {
                // nunca bloquear o request por falha no MDC
            }

            LoggerFactory.getLogger("request_summary")
                    .info("request finished");

            MDC.clear();
        }
    }

    /**
     * Pega o valor do header ou gera um novo ID.
     *
     * @param req
     * @param name
     * @return
     */
    private static String headerOrNew(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return v != null && !v.isBlank() ? v : newId();
    }

    /**
     * Gera um novo ID (UUID sem hífens).
     *
     * @return
     */
    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Retorna o primeiro valor não-nulo e não-vazio.
     *
     * @param values
     * @return
     */
    private static String firstNonEmpty(String... values) {
        for (String v : values)
            if (v != null && !v.isBlank())
                return v;
        return null;
    }

    /**
     * Garante que a string não é nula.
     *
     * @param v
     * @return
     */
    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
