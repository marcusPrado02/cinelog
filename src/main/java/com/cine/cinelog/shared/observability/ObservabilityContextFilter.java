package com.cine.cinelog.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component("observabilityRequestContextFilter") // <- nome único
@Order(5)
public class ObservabilityContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String traceId = firstNonBlank(req.getHeader("X-Trace-Id"), UUID.randomUUID().toString());
        String spanId = firstNonBlank(req.getHeader("X-Span-Id"), UUID.randomUUID().toString());
        String userIdS = req.getHeader("X-User-Id");
        String userEmail = req.getHeader("X-User-Email");
        String sourceApp = firstNonBlank(req.getHeader("X-Source-App"), "CineLog");
        String appVersion = firstNonBlank(req.getHeader("X-App-Version"), "v1");
        String txId = firstNonBlank(req.getHeader("X-Tx-Id"), UUID.randomUUID().toString());
        String userAgent = firstNonBlank(req.getHeader("User-Agent"), "unknown");
        String clientIp = firstNonBlank(req.getHeader("X-Forwarded-For"), req.getRemoteAddr());

        Long userId = null;
        try {
            if (userIdS != null)
                userId = Long.parseLong(userIdS);
        } catch (NumberFormatException ignored) {
        }

        var ctx = new RequestContext(userId, userEmail, sourceApp, appVersion, traceId, spanId, clientIp, userAgent,
                txId);
        RequestContext.set(ctx);

        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        try {
            chain.doFilter(req, res);
        } finally {
            RequestContext.clear();
            MDC.clear();
        }
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}