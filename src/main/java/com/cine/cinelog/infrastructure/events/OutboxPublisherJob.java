package com.cine.cinelog.infrastructure.events;

import com.cine.cinelog.infrastructure.messaging.outbox.OutboxMetrics;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Job agendado para processar eventos do outbox e publicá-los.
 *
 * <p>
 * Executa periodicamente:
 * </p>
 * <ol>
 * <li>Busca eventos PENDING ou FAILED prontos para retry</li>
 * <li>Publica cada evento (por enquanto apenas log, depois Kafka)</li>
 * <li>Marca como SENT ou FAILED com backoff exponencial</li>
 * </ol>
 *
 * <p>
 * Configurações recomendadas (application.yml):
 * </p>
 *
 * <pre>
 * outbox:
 *   publisher:
 *     enabled: true
 *     fixed-rate-ms: 5000  # executa a cada 5 segundos
 *     batch-size: 100      # processa até 100 eventos por vez
 * </pre>
 *
 * @since 1.1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherJob {

    private final OutboxEventRepository repository;
    private final OutboxEventPublisher eventPublisher;
    private final OutboxPublisherConfig config;
    private final OutboxMetrics metrics;

    public OutboxPublisherJob(
            OutboxEventRepository repository,
            OutboxEventPublisher eventPublisher,
            OutboxPublisherConfig config,
            OutboxMetrics metrics) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.config = config;
        this.metrics = metrics;
    }

    /**
     * Processa eventos pendentes do outbox.
     * Executa a cada fixedRateMs configurado (default 5000ms = 5s).
     */
    @Scheduled(fixedRateString = "${outbox.publisher.fixed-rate-ms:5000}", initialDelayString = "${outbox.publisher.initial-delay-ms:10000}")
    @Transactional
    public void processOutboxEvents() {
        try {
            var pageable = PageRequest.of(0, config.getBatchSize());
            var events = repository.findReadyForProcessing(Instant.now(), pageable);

            if (events.isEmpty()) {
                return;
            }

            log.info("Processing {} outbox events from batch", events.getNumberOfElements());

            int successCount = 0;
            int failureCount = 0;

            for (OutboxEventEntity event : events) {
                try {
                    processEvent(event);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to process outbox event: eventId={}, eventType={}, attempts={}, error={}",
                            event.getId(), event.getEventType(), event.getAttempts(), e.getMessage(), e);

                    // Marca como falho (com possível FAILED_PERM se atingiu max attempts)
                    event.markAsFailed(e.getMessage());
                    repository.save(event);

                    // Registra métrica
                    if (event.getStatus() == OutboxEventEntity.OutboxStatus.FAILED_PERM) {
                        metrics.recordFailedPermanently();
                    } else {
                        metrics.recordFailed();
                    }
                }
            }

            log.info("Outbox batch processed: success={}, failures={}, total={}",
                    successCount, failureCount, events.getNumberOfElements());

        } catch (Exception e) {
            log.error("Error processing outbox events batch: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa um evento individual.
     */
    private void processEvent(OutboxEventEntity event) {
        log.debug("Processing outbox event: eventId={}, eventType={}, attempt={}",
                event.getId(), event.getEventType(), event.getAttempts() + 1);

        // Delega publicação para o publisher (por enquanto log, depois Kafka)
        eventPublisher.publish(event);

        // Marca como enviado
        event.markAsSent();
        repository.save(event);

        // Registra métrica de sucesso
        metrics.recordPublished(event);

        log.info("Outbox event published successfully: eventId={}, eventType={}",
                event.getId(), event.getEventType());
    }

    /**
     * Job de limpeza (housekeeping) para remover eventos antigos já processados.
     * Executa diariamente à 1h da manhã.
     */
    @Scheduled(cron = "${outbox.housekeeping.cron:0 0 1 * * ?}")
    @Transactional
    public void cleanupOldEvents() {
        try {
            int retentionDays = config.getRetentionDays();
            Instant cutoff = Instant.now().minus(retentionDays, java.time.temporal.ChronoUnit.DAYS);

            log.info("Starting outbox housekeeping: removing SENT events older than {} days", retentionDays);

            var oldEvents = repository.findByStatusAndProcessedAtBefore(
                    OutboxEventEntity.OutboxStatus.SENT,
                    cutoff);

            if (!oldEvents.isEmpty()) {
                repository.deleteAll(oldEvents);
                log.info("Outbox housekeeping completed: removed {} old SENT events", oldEvents.size());
            } else {
                log.debug("Outbox housekeeping: no old events to remove");
            }

        } catch (Exception e) {
            log.error("Error during outbox housekeeping: {}", e.getMessage(), e);
        }
    }
}
