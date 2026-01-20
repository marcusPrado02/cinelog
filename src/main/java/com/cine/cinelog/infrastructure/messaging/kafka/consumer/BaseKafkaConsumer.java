package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

/**
 * Base class for Kafka consumers with correlation/tracing support.
 * <p>
 * Provides utility methods to:
 * - Extract correlationId from Kafka headers
 * - Extract traceparent (W3C Trace Context) from headers
 * - Set up MDC for logging
 * - Clean up MDC after processing
 * <p>
 * All Kafka consumers should extend this class and call:
 * - {@link #setupMDC(ConsumerRecord)} at the start of processing
 * - {@link #cleanupMDC()} in finally block
 * <p>
 * Related to:
 * - PR4: Observabilidade End-to-End (Tracing + Correlation)
 * - {@link com.cine.cinelog.infrastructure.web.filters.CorrelationIdFilter}
 * -
 * {@link com.cine.cinelog.infrastructure.messaging.kafka.KafkaEventPublisherAdapter}
 */
@Slf4j
public abstract class BaseKafkaConsumer {

    /**
     * Set up MDC with correlation/tracing info from Kafka headers.
     * <p>
     * Extracts:
     * - correlationId
     * - traceparent (W3C Trace Context)
     * - eventId (for event-specific logging)
     * <p>
     * Call this at the start of consumer processing.
     *
     * @param record Kafka ConsumerRecord
     */
    protected void setupMDC(ConsumerRecord<String, String> record) {
        // Extract correlationId from headers
        String correlationId = extractHeaderValue(record, "correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
            log.trace("Extracted correlationId from Kafka headers: {}", correlationId);
        } else {
            log.warn("No correlationId found in Kafka headers for topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        }

        // Extract traceparent (W3C Trace Context)
        String traceparent = extractHeaderValue(record, "traceparent");
        if (traceparent != null) {
            MDC.put("traceparent", traceparent);
            log.trace("Extracted traceparent from Kafka headers: {}", traceparent);
        }

        // Extract eventId for event-specific logging
        String eventId = extractHeaderValue(record, "eventId");
        if (eventId != null) {
            MDC.put("eventId", eventId);
        }

        // Extract eventType for context
        String eventType = extractHeaderValue(record, "eventType");
        if (eventType != null) {
            MDC.put("eventType", eventType);
        }

        log.debug(
                "Kafka consumer processing started: topic={}, partition={}, offset={}, correlationId={}, eventType={}",
                record.topic(), record.partition(), record.offset(), correlationId, eventType);
    }

    /**
     * Clean up MDC after consumer processing completes.
     * <p>
     * **CRITICAL:** Always call this in finally block to prevent memory leaks.
     */
    protected void cleanupMDC() {
        MDC.remove("correlationId");
        MDC.remove("traceparent");
        MDC.remove("eventId");
        MDC.remove("eventType");
        log.trace("MDC cleaned up after Kafka consumer processing");
    }

    /**
     * Extract string value from Kafka header.
     *
     * @param record     Kafka ConsumerRecord
     * @param headerName Header name
     * @return Header value as string, or null if not found
     */
    protected String extractHeaderValue(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Log consumer error with full context (topic, partition, offset,
     * correlationId).
     *
     * @param message   Error message
     * @param record    Kafka ConsumerRecord
     * @param exception Exception (optional)
     */
    protected void logConsumerError(String message, ConsumerRecord<String, String> record, Exception exception) {
        String correlationId = MDC.get("correlationId");
        if (exception != null) {
            log.error("{}: topic={}, partition={}, offset={}, correlationId={}, error={}",
                    message, record.topic(), record.partition(), record.offset(), correlationId,
                    exception.getMessage(), exception);
        } else {
            log.error("{}: topic={}, partition={}, offset={}, correlationId={}",
                    message, record.topic(), record.partition(), record.offset(), correlationId);
        }
    }
}
