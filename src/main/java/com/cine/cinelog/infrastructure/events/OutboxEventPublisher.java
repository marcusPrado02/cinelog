package com.cine.cinelog.infrastructure.events;

import com.cine.cinelog.infrastructure.messaging.kafka.KafkaEventPublisherAdapter;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Publisher de eventos do outbox.
 *
 * <p>
 * Integra com Kafka quando disponível, senão apenas loga.
 * </p>
 *
 * @since 1.1.0
 */
@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final KafkaEventPublisherAdapter kafkaPublisher;

    public OutboxEventPublisher(KafkaEventPublisherAdapter kafkaPublisher) {
        this.kafkaPublisher = kafkaPublisher;
    }

    /**
     * Publica evento (integra com Kafka se disponível).
     *
     * @param event Evento do outbox
     */
    public void publish(OutboxEventEntity event) {
        if (kafkaPublisher != null) {
            // Publica no Kafka
            kafkaPublisher.publish(event)
                    .exceptionally(ex -> {
                        log.error("Failed to publish to Kafka: eventId={}, will retry later",
                                event.getId(), ex);
                        throw new RuntimeException("Kafka publish failed", ex);
                    });
        } else {
            // Fallback: apenas loga (útil para testes sem Kafka)
            log.warn("[NO KAFKA] Would publish event: eventType={}, eventId={}, payload={}",
                    event.getEventType(),
                    event.getId(),
                    event.getPayload().substring(0, Math.min(200, event.getPayload().length())));
        }
    }
}
