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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para popular o MDC com dados de observabilidade (traceId, spanId,
 * userAgent, clientIp, userId, userEmail, etc) para todos os requests.
 * Também gera um log-resumo único por request.
 */
@Component
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

        // Diag SQL
        String diagHeader = req.getHeader("X-Diag-SQL");

        // User context (se você popular RequestContext em outro lugar)
        RequestContext ctx = RequestContext.get();
        String userId = ctx != null && ctx.userId != null ? String.valueOf(ctx.userId) : null;
        String userEmail = ctx != null ? ctx.userEmail : null;

        // Preenche MDC (vai para o JSON do Logback)
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("method", req.getMethod());
        MDC.put("path", req.getRequestURI());
        MDC.put("query", safe(req.getQueryString()));
        MDC.put("clientIp", clientIp);
        MDC.put("userAgent", userAgent);
        if (userId != null)
            MDC.put("userId", userId);
        if (userEmail != null)
            MDC.put("userEmail", userEmail);

        try {
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            long took = System.currentTimeMillis() - start;
            MDC.put("status", String.valueOf(res.getStatus()));
            MDC.put("tookMs", String.valueOf(took));

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
