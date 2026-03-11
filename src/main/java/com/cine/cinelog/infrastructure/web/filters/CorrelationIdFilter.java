package com.cine.cinelog.infrastructure.web.filters;

import com.cine.cinelog.shared.logging.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP Filter that ensures every request has a correlationId for distributed
 * tracing.
 * <p>
 * Responsibilities:
 * - Extract correlationId from request header (X-Correlation-Id)
 * - Generate new UUID if header is missing
 * - Store in MDC (SLF4J Mapped Diagnostic Context) for logging
 * - Add to response headers for client correlation
 * - Clean up MDC after request completes
 * <p>
 * This enables tracing a single user action across:
 * - HTTP request → Controller → Service → Outbox → Kafka → Consumer
 * <p>
 * Related to:
 * - PR4: Observabilidade End-to-End (Tracing + Correlation)
 * - OpenTelemetry W3C Trace Context
 *
 * @see com.cine.cinelog.shared.logging.CorrelationIdHolder
 * @see com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeFactory
 */
@Component
@Order(1) // Run early in filter chain
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * HTTP header name for correlation ID (industry standard).
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /**
     * MDC key for correlation ID (used in logback pattern).
     */
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = extractOrGenerateCorrelationId(request);

        try {
            // Store in MDC for logging (available to all log statements in this thread)
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

            // Store in ThreadLocal for programmatic access
            CorrelationIdHolder.set(correlationId);

            // Add to response headers (useful for clients to correlate requests)
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Log request start with correlationId
            if (log.isDebugEnabled()) {
                log.debug("HTTP Request started: method={}, uri={}, correlationId={}",
                        request.getMethod(), request.getRequestURI(), correlationId);
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Always clean up MDC and ThreadLocal (important for thread pool reuse)
            MDC.remove(MDC_CORRELATION_ID_KEY);
            CorrelationIdHolder.clear();

            if (log.isDebugEnabled()) {
                log.debug("HTTP Request completed: correlationId={}", correlationId);
            }
        }
    }

    /**
     * Extract correlationId from request header, or generate a new one.
     * <p>
     * Priority:
     * 1. X-Correlation-Id header (from upstream service or client)
     * 2. Generate new UUID
     *
     * @param request HTTP request
     * @return Correlation ID
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String headerValue = request.getHeader(CORRELATION_ID_HEADER);

        if (headerValue != null && !headerValue.isBlank()) {
            // Sanitize existing correlation ID from client/upstream to prevent log injection
            String sanitized = sanitizeForLogging(headerValue);

            if (sanitized == null || sanitized.isBlank()) {
                // If sanitization removes all characters, fall back to generating a new ID
                String newId = UUID.randomUUID().toString();
                if (log.isTraceEnabled()) {
                    log.trace("CorrelationId header was invalid after sanitization; generated new correlationId: {}", newId);
                }
                return newId;
            }

            if (log.isTraceEnabled()) {
                log.trace("Using existing correlationId from header: {}", sanitized);
            }
            return sanitized;
        } else {
            // Generate new correlation ID
            String newId = UUID.randomUUID().toString();
            if (log.isTraceEnabled()) {
                log.trace("Generated new correlationId: {}", newId);
            }
            return newId;
        }
    }

    /**
     * Sanitize a value before using it in logs or MDC.
     * Removes control characters (including new lines) and limits length to avoid log injection.
     */
    private String sanitizeForLogging(String value) {
        if (value == null) {
            return null;
        }
        // Remove all control characters (such as \r, \n, etc.)
        String sanitized = value.replaceAll("\\p{Cntrl}", "").trim();
        // Limit length to a reasonable maximum to avoid overly long log entries
        int maxLength = 128;
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    /**
     * Skip filter for actuator endpoints (reduce noise in logs).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}
