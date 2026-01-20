package com.cine.cinelog.shared.logging;

/**
 * ThreadLocal holder for correlationId.
 * <p>
 * Provides programmatic access to correlationId outside of logging context
 * (MDC).
 * Useful for:
 * - Adding correlationId to event metadata
 * - Including in Kafka headers
 * - Custom business logic that needs tracing info
 * <p>
 * **Important:** Always call {@link #clear()} in finally block to avoid memory
 * leaks
 * in thread pool environments.
 * <p>
 * Related to:
 * - PR4: Observabilidade End-to-End (Tracing + Correlation)
 * - {@link com.cine.cinelog.infrastructure.web.filters.CorrelationIdFilter}
 *
 * @see org.slf4j.MDC
 */
public final class CorrelationIdHolder {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private CorrelationIdHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Set correlationId for current thread.
     *
     * @param correlationId Correlation ID (usually UUID)
     */
    public static void set(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    /**
     * Get correlationId for current thread.
     *
     * @return Correlation ID, or null if not set
     */
    public static String get() {
        return CORRELATION_ID.get();
    }

    /**
     * Clear correlationId for current thread.
     * <p>
     * **CRITICAL:** Always call this in finally block to prevent memory leaks
     * when using thread pools.
     */
    public static void clear() {
        CORRELATION_ID.remove();
    }

    /**
     * Check if correlationId is set for current thread.
     *
     * @return true if set, false otherwise
     */
    public static boolean isSet() {
        return CORRELATION_ID.get() != null;
    }
}
