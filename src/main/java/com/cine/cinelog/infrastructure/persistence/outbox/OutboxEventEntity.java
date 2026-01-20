package com.cine.cinelog.infrastructure.persistence.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA para tabela outbox_event.
 *
 * <p>
 * Implementa o Outbox Pattern para publicação confiável de Domain Events.
 * Eventos são gravados transacionalmente junto com agregados e processados
 * assincronamente por um job.
 * </p>
 *
 * @since 1.1.0
 */
@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_outbox_event_type", columnList = "event_type"),
        @Index(name = "idx_outbox_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
    }

    /**
     * Marca evento como enviado com sucesso.
     */
    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = Instant.now();
    }

    /**
     * Marca evento como falho e calcula próximo retry com backoff exponencial +
     * jitter.
     *
     * @param error            Mensagem de erro
     * @param baseDelaySeconds Delay base em segundos (default 60)
     * @param maxDelaySeconds  Delay máximo em segundos (default 3600 = 1h)
     * @param maxAttempts      Número máximo de tentativas antes de FAILED_PERM
     */
    public void markAsFailed(String error, long baseDelaySeconds, long maxDelaySeconds, int maxAttempts) {
        this.attempts++;
        this.lastError = error != null && error.length() > 5000
                ? error.substring(0, 5000)
                : error;

        // Se excedeu máximo de tentativas → FAILED_PERM
        if (this.attempts >= maxAttempts) {
            this.status = OutboxStatus.FAILED_PERM;
            this.nextRetryAt = null; // Não há próximo retry
            return;
        }

        // Senão → FAILED com retry agendado
        this.status = OutboxStatus.FAILED;

        // Backoff exponencial: baseDelay * 2^attempts
        long exponentialDelay = baseDelaySeconds * (long) Math.pow(2, Math.min(attempts - 1, 10));

        // Limitar por maxDelay
        long cappedDelay = Math.min(exponentialDelay, maxDelaySeconds);

        // Adicionar jitter (±10%) para evitar thundering herd
        long jitter = (long) (cappedDelay * 0.1 * (Math.random() * 2 - 1));
        long finalDelay = Math.max(baseDelaySeconds, cappedDelay + jitter);

        this.nextRetryAt = Instant.now().plusSeconds(finalDelay);
    }

    /**
     * Marca evento como falho com parâmetros padrão.
     * - Base delay: 60s
     * - Max delay: 3600s (1 hora)
     * - Max attempts: 5
     */
    public void markAsFailed(String error) {
        markAsFailed(error, 60, 3600, 5);
    }

    /**
     * Marca evento como falho permanente (sem mais retries).
     */
    public void markAsFailedPermanently(String error) {
        this.status = OutboxStatus.FAILED_PERM;
        this.lastError = error != null && error.length() > 5000
                ? error.substring(0, 5000)
                : error;
        this.nextRetryAt = null;
    }

    /**
     * Verifica se evento está pronto para retry.
     */
    public boolean isReadyForRetry() {
        return status == OutboxStatus.FAILED
                && nextRetryAt != null
                && Instant.now().isAfter(nextRetryAt);
    }

    public enum OutboxStatus {
        /**
         * Evento aguardando processamento inicial.
         */
        PENDING,

        /**
         * Evento publicado com sucesso no Kafka.
         */
        SENT,

        /**
         * Evento falhou, mas ainda pode ser retentado.
         * Será processado novamente quando next_retry_at for atingido.
         */
        FAILED,

        /**
         * Evento falhou permanentemente após N tentativas.
         * Não será mais processado automaticamente.
         * Requer investigação manual ou replay via admin endpoint.
         */
        FAILED_PERM
    }
}
