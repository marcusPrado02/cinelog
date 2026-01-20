package com.cine.cinelog.infrastructure.persistence.outbox;

import com.cine.cinelog.core.domain.events.DomainEvent;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper para converter DomainEvent -> OutboxEventEntity.
 *
 * <p>
 * Desde a versão 1.2.0, envelopa eventos com EventEnvelope para:
 * <ul>
 * <li>Rastreabilidade (correlationId, causationId, traceparent)</li>
 * <li>Versionamento (schema version)</li>
 * <li>Validação padronizada no consumer</li>
 * </ul>
 * </p>
 *
 * @since 1.1.0
 */
@Component
public class OutboxEventMapper {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventMapper.class);
    private final ObjectMapper objectMapper;
    private final EventEnvelopeFactory envelopeFactory;

    public OutboxEventMapper(ObjectMapper objectMapper, EventEnvelopeFactory envelopeFactory) {
        this.objectMapper = objectMapper;
        this.envelopeFactory = envelopeFactory;
    }

    /**
     * Converte DomainEvent para OutboxEventEntity.
     * Serializa evento dentro de um EventEnvelope (formato padronizado).
     *
     * @param event DomainEvent
     * @return OutboxEventEntity com payload = EventEnvelope(event)
     */
    public OutboxEventEntity toEntity(DomainEvent event) {
        try {
            // Criar envelope com metadados do contexto (MDC)
            EventEnvelope<DomainEvent> envelope = envelopeFactory.create(
                    event.type(),
                    event.version(),
                    event);

            // Serializar envelope completo como JSON
            String envelopeJson = objectMapper.writeValueAsString(envelope);

            return OutboxEventEntity.builder()
                    .id(event.eventId())
                    .aggregateType(event.aggregateType())
                    .aggregateId(event.aggregateId())
                    .eventType(event.type())
                    .eventVersion(event.version())
                    .occurredAt(event.occurredAt())
                    .payload(envelopeJson) // Agora payload é EventEnvelope JSON
                    .status(OutboxEventEntity.OutboxStatus.PENDING)
                    .attempts(0)
                    .build();

        } catch (Exception e) {
            log.error("Failed to serialize DomainEvent to EventEnvelope JSON: {}", event, e);
            throw new RuntimeException("Failed to serialize event for outbox: " + event.type(), e);
        }
    }

    /**
     * Desserializa payload JSON de volta para DomainEvent (se necessário).
     * Útil para replay ou debugging.
     *
     * <p>
     * <strong>Nota:</strong> Desde 1.2.0, o payload é um EventEnvelope.
     * Este método extrai o DomainEvent do campo envelope.payload.
     * </p>
     *
     * @param entity     OutboxEventEntity
     * @param eventClass Classe do evento
     * @return DomainEvent (extraído do envelope)
     */
    public <T extends DomainEvent> T toDomainEvent(OutboxEventEntity entity, Class<T> eventClass) {
        try {
            // Deserializar envelope
            EventEnvelope<?> envelope = objectMapper.readValue(
                    entity.getPayload(),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, eventClass));

            // Extrair payload do envelope
            Object payload = envelope.getPayload();
            return objectMapper.convertValue(payload, eventClass);

        } catch (Exception e) {
            log.error("Failed to deserialize OutboxEvent payload to {}: {}",
                    eventClass.getName(), entity.getPayload(), e);
            throw new RuntimeException("Failed to deserialize outbox event: " + entity.getId(), e);
        }
    }
}
