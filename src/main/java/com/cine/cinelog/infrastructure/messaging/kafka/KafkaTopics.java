package com.cine.cinelog.infrastructure.messaging.kafka;

/**
 * Definição centralizada de tópicos Kafka do sistema.
 *
 * <p>
 * Convenção de nomenclatura: cinelog.{aggregate}.{event}.v{version}
 * </p>
 *
 * @since 1.1.0
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Utility class
    }

    // Watch Entry Events
    public static final String WATCH_ENTRY_CREATED_V1 = "cinelog.watchentry.created.v1";
    public static final String WATCH_ENTRY_RATED_V1 = "cinelog.watchentry.rated.v1";

    // DLQ (Dead Letter Queue) - eventos que falharam após todos os retries
    public static final String DLQ = "cinelog.dlq";

    /**
     * Determina tópico baseado no tipo de evento.
     */
    public static String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "watchentry.created" -> WATCH_ENTRY_CREATED_V1;
            case "watchentry.rated" -> WATCH_ENTRY_RATED_V1;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
