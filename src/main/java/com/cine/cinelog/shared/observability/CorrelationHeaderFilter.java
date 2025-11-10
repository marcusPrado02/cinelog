package com.cine.cinelog.shared.observability;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para adicionar cabeçalhos de correlação e rastreamento às respostas
 * HTTP.
 * 
 * Adiciona cabeçalhos X-Trace-Id, X-Span-Id, X-Request-Id e X-Tx-Id às
 * respostas.
 */
@Component
public class CorrelationHeaderFilter implements Filter {

    private final Tracer tracer;

    public CorrelationHeaderFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpRes = (HttpServletResponse) res;
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            var span = tracer.currentSpan();
            if (span != null) {
                httpRes.setHeader("X-Trace-Id", span.context().traceId());
                httpRes.setHeader("X-Span-Id", span.context().spanId());
            }
            httpRes.setHeader("X-Request-Id", requestId);
            var txId = MDC.get("txId");
            if (txId != null)
                httpRes.setHeader("X-Tx-Id", txId);
            MDC.remove("requestId");
        }
    }
}
