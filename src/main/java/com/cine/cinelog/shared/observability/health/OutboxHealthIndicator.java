package com.cine.cinelog.shared.observability.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Health indicator para a fila de eventos outbox (Outbox Pattern).
 *
 * <p>
 * <strong>A10:2025 — Mishandling of Exceptional Conditions</strong>
 * </p>
 *
 * <p>
 * Verifica o estado da fila de outbox events monitorando:
 * <ul>
 * <li><strong>Eventos pendentes</strong>: quantidade de eventos com status
 * PENDING
 * aguardando publicação no Kafka.</li>
 * <li><strong>Eventos com falha permanente</strong>: quantidade de eventos com
 * status FAILED_PERM que falharam após todas as tentativas de retry.</li>
 * <li><strong>Alerta de acúmulo</strong>: se o número de pendentes excede um
 * threshold (100), indica possível problema no publisher ou no Kafka.</li>
 * </ul>
 *
 * <p>
 * Este indicador ajuda a detectar cenários onde exceções no pipeline de
 * eventos estão sendo engolidas silenciosamente, causando acúmulo na outbox.
 * </p>
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxHealthIndicator implements HealthIndicator {

    private static final int PENDING_THRESHOLD = 100;
    private static final int FAILED_THRESHOLD = 10;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            Integer pending = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_event WHERE status = 'PENDING'",
                    Integer.class);
            Integer failed = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_event WHERE status = 'FAILED_PERM'",
                    Integer.class);

            int pendingCount = pending != null ? pending : 0;
            int failedCount = failed != null ? failed : 0;

            var builder = Health.up()
                    .withDetail("pendingEvents", pendingCount)
                    .withDetail("failedPermanentEvents", failedCount)
                    .withDetail("pendingThreshold", PENDING_THRESHOLD);

            if (failedCount > FAILED_THRESHOLD) {
                return builder.down()
                        .withDetail("reason",
                                "Excesso de eventos com falha permanente: " + failedCount)
                        .build();
            }

            if (pendingCount > PENDING_THRESHOLD) {
                return builder.down()
                        .withDetail("reason",
                                "Acúmulo de eventos pendentes: " + pendingCount
                                        + " (threshold=" + PENDING_THRESHOLD + ")")
                        .build();
            }

            return builder.build();
        } catch (Exception ex) {
            log.warn("Erro ao verificar saúde da outbox", ex);
            return Health.down()
                    .withDetail("error", "Não foi possível consultar outbox: " + ex.getMessage())
                    .build();
        }
    }
}
