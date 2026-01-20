package com.cine.cinelog.infrastructure.persistence.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA para a tabela inbox_event.
 *
 * <p>
 * Implementa o pattern Idempotent Consumer para garantir exactly-once semantics
 * no processamento de eventos Kafka, mesmo com at-least-once delivery.
 * </p>
 *
 * <p>
 * O consumer verifica se o eventId já existe antes de processar. Se existe,
 * descarta (duplicate delivery). Se não existe, processa e insere
 * transacionalmente.
 * </p>
 *
 * <p>
 * <strong>Fluxo:</strong>
 * </p>
 * 
 * <pre>
 * 1. Consumer recebe evento do Kafka
 * 2. Verifica existencia na inbox por eventId
 * 3. Se já existe → skip (já processado)
 * 4. Se não existe → processa + insere inbox (transação única)
 * 5. Commit manual do offset Kafka
 * </pre>
 */
@Entity
@Table(name = "inbox_event")
public class InboxEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload;

    /**
     * Construtor padrão para JPA.
     */
    protected InboxEventEntity() {
    }

    /**
     * Construtor completo para criação programática.
     */
    public InboxEventEntity(
            UUID eventId,
            String consumerName,
            String eventType,
            String aggregateId,
            Instant receivedAt,
            String payload) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.receivedAt = receivedAt;
        this.payload = payload;
    }

    /**
     * Marca o evento como processado com timestamp atual.
     */
    public void markAsProcessed() {
        this.processedAt = Instant.now();
    }

    /**
     * Verifica se o evento já foi processado.
     *
     * @return true se processedAt não é null
     */
    public boolean isProcessed() {
        return this.processedAt != null;
    }

    // Getters
    public UUID getEventId() {
        return eventId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getPayload() {
        return payload;
    }
}
