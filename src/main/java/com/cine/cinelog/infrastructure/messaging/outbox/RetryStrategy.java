package com.cine.cinelog.infrastructure.messaging.outbox;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Estratégia de retry com backoff exponencial e jitter.
 *
 * <p>
 * Implementa o padrão de retry robusto com:
 * </p>
 * <ul>
 * <li><strong>Backoff Exponencial:</strong> delay = baseDelay * 2^attempts</li>
 * <li><strong>Jitter:</strong> ±10% de variação aleatória para evitar
 * thundering herd</li>
 * <li><strong>Cap máximo:</strong> Limita delay para não crescer
 * indefinidamente</li>
 * <li><strong>Falha permanente:</strong> Após N tentativas, para de
 * retentar</li>
 * </ul>
 *
 * <p>
 * <strong>Exemplo de uso:</strong>
 * </p>
 *
 * <pre>
 * RetryStrategy strategy = RetryStrategy.builder()
 *         .baseDelaySeconds(60)
 *         .maxDelaySeconds(3600)
 *         .maxAttempts(5)
 *         .jitterPercent(10.0)
 *         .build();
 *
 * RetryDecision decision = strategy.calculateNextRetry(currentAttempts);
 * if (decision.shouldRetry()) {
 *     scheduleRetryAt(decision.getNextRetryAt());
 * } else {
 *     markAsFailedPermanently();
 * }
 * </pre>
 *
 * @since 1.2.0 (PR2)
 */
@Getter
@Builder
public class RetryStrategy {

    /**
     * Delay inicial em segundos (padrão: 60s = 1 minuto).
     */
    @Builder.Default
    private final long baseDelaySeconds = 60;

    /**
     * Delay máximo em segundos (padrão: 3600s = 1 hora).
     */
    @Builder.Default
    private final long maxDelaySeconds = 3600;

    /**
     * Número máximo de tentativas antes de desistir (padrão: 5).
     */
    @Builder.Default
    private final int maxAttempts = 5;

    /**
     * Percentual de jitter a aplicar (padrão: 10%).
     * Jitter evita thundering herd quando múltiplos eventos falham simultaneamente.
     */
    @Builder.Default
    private final double jitterPercent = 10.0;

    /**
     * Calcula a decisão de retry baseado no número de tentativas atual.
     *
     * @param currentAttempts Número de tentativas já realizadas
     * @return Decisão de retry com nextRetryAt ou indicação de falha permanente
     */
    public RetryDecision calculateNextRetry(int currentAttempts) {
        int nextAttempt = currentAttempts + 1;

        // Se atingiu o máximo → falha permanente
        if (nextAttempt >= maxAttempts) {
            return RetryDecision.failedPermanently(maxAttempts);
        }

        // Calcula delay com backoff exponencial
        long exponentialDelay = baseDelaySeconds * (long) Math.pow(2, currentAttempts);

        // Aplica cap máximo
        long cappedDelay = Math.min(exponentialDelay, maxDelaySeconds);

        // Adiciona jitter (±jitterPercent%)
        double jitterFactor = (jitterPercent / 100.0) * (Math.random() * 2 - 1);
        long jitter = (long) (cappedDelay * jitterFactor);
        long finalDelay = Math.max(baseDelaySeconds, cappedDelay + jitter);

        Instant nextRetryAt = Instant.now().plusSeconds(finalDelay);

        return RetryDecision.retry(nextRetryAt, nextAttempt, finalDelay);
    }

    /**
     * Calcula delay total até determinada tentativa (útil para estimativas).
     *
     * @param attemptNumber Número da tentativa
     * @return Delay aproximado em segundos (sem jitter)
     */
    public long estimateDelayForAttempt(int attemptNumber) {
        if (attemptNumber <= 0) {
            return 0;
        }
        long exponentialDelay = baseDelaySeconds * (long) Math.pow(2, attemptNumber - 1);
        return Math.min(exponentialDelay, maxDelaySeconds);
    }

    /**
     * Decisão de retry.
     */
    @Getter
    @Builder
    public static class RetryDecision {
        private final boolean shouldRetry;
        private final Instant nextRetryAt;
        private final int nextAttempt;
        private final long delaySeconds;
        private final String reason;

        public static RetryDecision retry(Instant nextRetryAt, int nextAttempt, long delaySeconds) {
            return RetryDecision.builder()
                    .shouldRetry(true)
                    .nextRetryAt(nextRetryAt)
                    .nextAttempt(nextAttempt)
                    .delaySeconds(delaySeconds)
                    .reason("Retry scheduled with exponential backoff")
                    .build();
        }

        public static RetryDecision failedPermanently(int maxAttempts) {
            return RetryDecision.builder()
                    .shouldRetry(false)
                    .nextRetryAt(null)
                    .nextAttempt(maxAttempts)
                    .delaySeconds(0)
                    .reason("Max attempts (" + maxAttempts + ") reached - marked as FAILED_PERM")
                    .build();
        }
    }
}
