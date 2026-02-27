package com.cine.cinelog.shared.observability.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A09:2025 — Serviço de detecção de padrões e alertas de segurança.
 *
 * <h3>Por que alertas automatizados?</h3>
 * <p>
 * Logs sem alertas são como câmeras de segurança sem monitor —
 * eles gravam o incidente, mas ninguém descobre até ser tarde demais.
 * Segundo o <b>IBM Cost of a Data Breach 2024</b>, o tempo médio para
 * identificar uma breach é <b>194 dias</b>. Com alertas bem configurados,
 * esse tempo cai para horas ou minutos.
 * </p>
 *
 * <h3>Como funciona?</h3>
 * <ol>
 * <li>O {@link SecurityEventLogger} chama {@link #evaluate(SecurityEvent, Map)}
 * para cada evento alertável.</li>
 * <li>Este serviço mantém contadores deslizantes (sliding window)
 * por tipo de evento.</li>
 * <li>Quando o threshold é atingido dentro da janela de tempo,
 * uma ação de alerta é disparada.</li>
 * </ol>
 *
 * <h3>Thresholds configuráveis</h3>
 * <ul>
 * <li>{@code AUTH_FAILURE}: 10 falhas em 5 minutos → possível brute force</li>
 * <li>{@code SQL_INJECTION_ATTEMPT}: 3 em 1 minuto → ataque ativo</li>
 * <li>{@code RATE_LIMITED}: 50 em 5 minutos → DDoS ou scraping</li>
 * <li>{@code TAMPER_DETECTED}: 1 → sempre alerta (zero tolerance)</li>
 * </ul>
 *
 * <h3>Ações de alerta</h3>
 * <p>
 * Atualmente: log de nível ERROR com marker {@code SECURITY_ALERT} +
 * incremento de métrica Micrometer {@code cinelog.security.alerts_total}.
 * Extensível para: webhook (Slack, PagerDuty), email, Kafka topic.
 * </p>
 *
 * @since 1.2
 * @see SecurityEvent
 * @see SecurityEventLogger
 */
@Service
@Slf4j
public class SecurityAlertService {

    private final MeterRegistry meterRegistry;

    /** Janela de avaliação em segundos. */
    private final long windowSeconds;

    /** Contadores deslizantes por tipo de evento. */
    private final Map<SecurityEvent, SlidingWindowCounter> eventCounters = new ConcurrentHashMap<>();

    /**
     * Thresholds por tipo de evento: quantos eventos na janela para disparar
     * alerta.
     */
    private final Map<SecurityEvent, Integer> thresholds;

    public SecurityAlertService(
            MeterRegistry meterRegistry,
            @Value("${cinelog.security.alerting.window-seconds:300}") long windowSeconds,
            @Value("${cinelog.security.alerting.threshold-auth-failure:10}") int thresholdAuthFailure,
            @Value("${cinelog.security.alerting.threshold-sqli:3}") int thresholdSqli,
            @Value("${cinelog.security.alerting.threshold-rate-limit:50}") int thresholdRateLimit,
            @Value("${cinelog.security.alerting.threshold-tamper:1}") int thresholdTamper) {
        this.meterRegistry = meterRegistry;
        this.windowSeconds = windowSeconds;

        this.thresholds = Map.ofEntries(
                Map.entry(SecurityEvent.AUTH_FAILURE, thresholdAuthFailure),
                Map.entry(SecurityEvent.AUTH_LOCKED, 3),
                Map.entry(SecurityEvent.JWT_INVALID, 10),
                Map.entry(SecurityEvent.TOKEN_REUSE_DETECTED, 1),
                Map.entry(SecurityEvent.ACCESS_DENIED, 20),
                Map.entry(SecurityEvent.PRIVILEGE_ESCALATION, 1),
                Map.entry(SecurityEvent.RATE_LIMITED, thresholdRateLimit),
                Map.entry(SecurityEvent.SQL_INJECTION_ATTEMPT, thresholdSqli),
                Map.entry(SecurityEvent.TAMPER_DETECTED, thresholdTamper),
                Map.entry(SecurityEvent.INTEGRITY_VIOLATION, 1),
                Map.entry(SecurityEvent.SECURITY_CONFIG_CHANGE, 5),
                Map.entry(SecurityEvent.SUSPICIOUS_ACTIVITY, 1));
    }

    /**
     * Avalia se o evento atingiu o threshold e dispara alerta se necessário.
     *
     * @param event      tipo do evento
     * @param structured payload estruturado (usado para contexto no alerta)
     */
    public void evaluate(SecurityEvent event, Map<String, Object> structured) {
        // Incrementa Micrometer counter por tipo de evento
        incrementMetric(event);

        // Verifica threshold
        Integer threshold = thresholds.get(event);
        if (threshold == null) {
            return; // Evento sem threshold definido
        }

        SlidingWindowCounter counter = eventCounters.computeIfAbsent(
                event, e -> new SlidingWindowCounter());
        int count = counter.incrementAndGet(windowSeconds);

        if (count >= threshold) {
            triggerAlert(event, count, structured);
            counter.reset(); // Reset para não alertar repetidamente no mesmo surto
        }
    }

    /**
     * Dispara alerta de segurança.
     *
     * <p>
     * Ação atual: log ERROR + métrica. Extensível para webhook.
     * </p>
     */
    private void triggerAlert(SecurityEvent event, int count, Map<String, Object> context) {
        log.error(
                "🚨 SECURITY ALERT: {} — {} ocorrências em {} segundos. "
                        + "Threshold: {}. Contexto: {}",
                event.getCode() + " " + event.getDescription(),
                count,
                windowSeconds,
                thresholds.get(event),
                context);

        Counter.builder("cinelog.security.alerts_total")
                .description("Alertas de segurança disparados")
                .tags(List.of(
                        Tag.of("event", event.name()),
                        Tag.of("severity", event.getSeverity().name())))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Incrementa métricas Micrometer por tipo de evento de segurança.
     */
    private void incrementMetric(SecurityEvent event) {
        Counter.builder("cinelog.security.events_total")
                .description("Total de eventos de segurança por tipo")
                .tags(List.of(
                        Tag.of("event", event.name()),
                        Tag.of("severity", event.getSeverity().name()),
                        Tag.of("code", event.getCode())))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Contador com janela deslizante simples (resets após expiração).
     *
     * <p>
     * Em produção com múltiplas instâncias, substituir por Redis
     * com TTL automático.
     * </p>
     */
    private static class SlidingWindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();

        int incrementAndGet(long windowSeconds) {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plusSeconds(windowSeconds))) {
                // Janela expirou, reseta
                count.set(1);
                windowStart = now;
                return 1;
            }
            return count.incrementAndGet();
        }

        void reset() {
            count.set(0);
            windowStart = Instant.now();
        }
    }
}
