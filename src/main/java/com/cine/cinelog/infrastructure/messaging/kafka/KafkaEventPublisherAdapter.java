package com.cine.cinelog.infrastructure.messaging.kafka;

import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter para publicar eventos do Outbox no Kafka.
 *
 * <p>
 * Substitui o OutboxEventPublisher quando Kafka está ativo.
 * </p>
 *
 * @since 1.1.0
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisherAdapter {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica evento do outbox no Kafka.
     *
     * <p>
     * <strong>Formato:</strong> O payload é um EventEnvelope JSON que já contém
     * metadados (eventId, type, version, metadata, payload). Os headers Kafka
     * duplicam algumas informações para compatibilidade e filtros de brokers.
     * </p>
     *
     * @param event OutboxEventEntity (com payload = EventEnvelope JSON)
     * @return CompletableFuture com resultado do envio
     */
    public CompletableFuture<SendResult<String, String>> publish(OutboxEventEntity event) {
        String topic = KafkaTopics.getTopicForEventType(event.getEventType());
        String key = event.getAggregateId(); // Garante ordem para mesmo agregado
        String value = event.getPayload(); // Payload já é EventEnvelope JSON

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        // Headers Kafka para rastreamento e compatibilidade (duplicam informações do
        // envelope)
        // Consumer pode usar headers para filtros sem deserializar payload
        record.headers().add(new RecordHeader("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventVersion",
                String.valueOf(event.getEventVersion()).getBytes(StandardCharsets.UTF_8)));
        record.headers()
                .add(new RecordHeader("aggregateType", event.getAggregateType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("aggregateId", event.getAggregateId().getBytes(StandardCharsets.UTF_8)));

        // Add correlation metadata to Kafka headers for tracing (PR4)
        addCorrelationHeaders(record, event);

        log.debug("Sending EventEnvelope to Kafka: topic={}, eventId={}, aggregateId={}, type={}",
                topic, event.getId(), event.getAggregateId(), event.getEventType());

        return kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send EventEnvelope to Kafka: eventId={}, topic={}, error={}",
                                event.getId(), topic, ex.getMessage(), ex);
                    } else {
                        log.info(
                                "EventEnvelope sent to Kafka successfully: eventId={}, topic={}, partition={}, offset={}",
                                event.getId(), topic, result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Extract correlationId and traceparent from EventEnvelope payload and add to
     * Kafka headers.
     * <p>
     * This enables distributed tracing across HTTP → Outbox → Kafka → Consumer.
     * <p>
     * Related to PR4: Observabilidade End-to-End
     *
     * @param record Kafka ProducerRecord
     * @param event  OutboxEventEntity (payload contains EventEnvelope JSON)
     */
    private void addCorrelationHeaders(ProducerRecord<String, String> record, OutboxEventEntity event) {
        try {
            // Parse EventEnvelope JSON to extract metadata
            JsonNode envelopeNode = objectMapper.readTree(event.getPayload());
            JsonNode metadataNode = envelopeNode.get("metadata");

            if (metadataNode != null && metadataNode.isObject()) {
                // Extract correlationId from metadata
                JsonNode correlationIdNode = metadataNode.get("correlationId");
                if (correlationIdNode != null && !correlationIdNode.isNull()) {
                    String correlationId = correlationIdNode.asText();
                    record.headers().add(new RecordHeader("correlationId",
                            correlationId.getBytes(StandardCharsets.UTF_8)));
                    log.trace("Added correlationId to Kafka headers: {}", correlationId);
                } else {
                    log.warn("No correlationId found in EventEnvelope metadata for eventId={}", event.getId());
                }

                // Extract traceparent (W3C Trace Context) from metadata
                JsonNode traceparentNode = metadataNode.get("traceparent");
                if (traceparentNode != null && !traceparentNode.isNull()) {
                    String traceparent = traceparentNode.asText();
                    record.headers().add(new RecordHeader("traceparent",
                            traceparent.getBytes(StandardCharsets.UTF_8)));
                    log.trace("Added traceparent to Kafka headers: {}", traceparent);
                }

                // Extract causationId if present (event causality chain)
                JsonNode causationIdNode = metadataNode.get("causationId");
                if (causationIdNode != null && !causationIdNode.isNull()) {
                    String causationId = causationIdNode.asText();
                    record.headers().add(new RecordHeader("causationId",
                            causationId.getBytes(StandardCharsets.UTF_8)));
                    log.trace("Added causationId to Kafka headers: {}", causationId);
                }
            } else {
                log.warn("No metadata found in EventEnvelope for eventId={}", event.getId());
            }

        } catch (Exception e) {
            // Don't fail the publish if correlation header extraction fails
            log.error("Failed to extract correlation headers from EventEnvelope: eventId={}, error={}",
                    event.getId(), e.getMessage(), e);
        }
    }
}
