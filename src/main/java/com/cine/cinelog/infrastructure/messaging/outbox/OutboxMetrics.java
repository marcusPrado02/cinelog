package com.cine.cinelog.infrastructure.messaging.outbox;

import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Métricas do Outbox Pattern para Prometheus/Grafana.
 *
 * <p>
 * Expõe métricas críticas para observabilidade:
 * </p>
 * <ul>
 * <li><strong>Gauges:</strong> outbox.pending.count, outbox.failed.count,
 * outbox.failed_perm.count</li>
 * <li><strong>Counters:</strong> outbox.published.total,
 * outbox.failed.total</li>
 * <li><strong>Timers:</strong> outbox.publish.latency (tempo desde criação até
 * publicação)</li>
 * </ul>
 *
 * <p>
 * Acesso via: http://localhost:8080/actuator/prometheus
 * </p>
 *
 * @since 1.2.0 (PR2)
 */
@Slf4j
@Component
public class OutboxMetrics {

    private final OutboxEventRepository repository;
    private final MeterRegistry meterRegistry;

    // Gauges
    private final Gauge pendingGauge;
    private final Gauge failedGauge;
    private final Gauge failedPermGauge;

    // Counters
    private final Counter publishedCounter;
    private final Counter failedCounter;
    private final Counter failedPermCounter;

    // Timer
    private final Timer publishLatencyTimer;

    public OutboxMetrics(OutboxEventRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;

        // Registrar Gauges (valores atuais)
        this.pendingGauge = Gauge.builder("outbox.pending.count", repository,
                repo -> repo.countByStatus(OutboxEventEntity.OutboxStatus.PENDING))
                .description("Number of PENDING outbox events waiting to be published")
                .tag("status", "pending")
                .register(meterRegistry);

        this.failedGauge = Gauge.builder("outbox.failed.count", repository,
                repo -> repo.countByStatus(OutboxEventEntity.OutboxStatus.FAILED))
                .description("Number of FAILED outbox events that will be retried")
                .tag("status", "failed")
                .register(meterRegistry);

        this.failedPermGauge = Gauge.builder("outbox.failed_perm.count", repository,
                repo -> repo.countByStatus(OutboxEventEntity.OutboxStatus.FAILED_PERM))
                .description("Number of FAILED_PERM outbox events (require manual intervention)")
                .tag("status", "failed_perm")
                .register(meterRegistry);

        // Registrar Counters (totais acumulados)
        this.publishedCounter = Counter.builder("outbox.published.total")
                .description("Total number of successfully published outbox events")
                .tag("outcome", "success")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("outbox.failed.total")
                .description("Total number of failed outbox publish attempts")
                .tag("outcome", "failure")
                .register(meterRegistry);

        this.failedPermCounter = Counter.builder("outbox.failed_perm.total")
                .description("Total number of permanently failed outbox events")
                .tag("outcome", "failed_perm")
                .register(meterRegistry);

        // Registrar Timer (latência de publicação)
        this.publishLatencyTimer = Timer.builder("outbox.publish.latency")
                .description("Time from event creation to successful publication")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        log.info("OutboxMetrics initialized and registered with MeterRegistry");
    }

    /**
     * Registra publicação bem-sucedida.
     *
     * @param event Evento publicado
     */
    public void recordPublished(OutboxEventEntity event) {
        publishedCounter.increment();

        // Calcula latência (createdAt → processedAt)
        if (event.getCreatedAt() != null && event.getProcessedAt() != null) {
            Duration latency = Duration.between(event.getCreatedAt(), event.getProcessedAt());
            publishLatencyTimer.record(latency.toMillis(), TimeUnit.MILLISECONDS);
        }

        log.debug("Metrics recorded for published event: eventId={}, latency={}ms",
                event.getId(),
                event.getProcessedAt() != null && event.getCreatedAt() != null
                        ? Duration.between(event.getCreatedAt(), event.getProcessedAt()).toMillis()
                        : "N/A");
    }

    /**
     * Registra falha (temporária).
     */
    public void recordFailed() {
        failedCounter.increment();
    }

    /**
     * Registra falha permanente.
     */
    public void recordFailedPermanently() {
        failedPermCounter.increment();
    }

    /**
     * Retorna métricas atuais (útil para health checks).
     */
    public OutboxMetricsSnapshot getSnapshot() {
        return new OutboxMetricsSnapshot(
                repository.countByStatus(OutboxEventEntity.OutboxStatus.PENDING),
                repository.countByStatus(OutboxEventEntity.OutboxStatus.FAILED),
                repository.countByStatus(OutboxEventEntity.OutboxStatus.FAILED_PERM),
                (long) publishedCounter.count(),
                (long) failedCounter.count(),
                (long) failedPermCounter.count());
    }

    /**
     * Snapshot de métricas.
     */
    public record OutboxMetricsSnapshot(
            long pendingCount,
            long failedCount,
            long failedPermCount,
            long totalPublished,
            long totalFailed,
            long totalFailedPerm) {

        public boolean hasProblems() {
            return failedPermCount > 0 || failedCount > 100;
        }
    }
}
