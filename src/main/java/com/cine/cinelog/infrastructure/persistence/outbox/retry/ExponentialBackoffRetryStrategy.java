package com.cine.cinelog.infrastructure.persistence.outbox.retry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementa retry com exponential backoff + jitter.
 *
 * <p>
 * <strong>Exponential Backoff</strong>:
 * 
 * <pre>
 * delay = baseDelay * 2^attempt (limitado por maxDelay)
 *
 * Exemplo (baseDelay=1s, maxDelay=3600s):
 * - Attempt 0: 1s
 * - Attempt 1: 2s
 * - Attempt 2: 4s
 * - Attempt 3: 8s
 * - Attempt 4: 16s
 * - Attempt 5: 32s
 * - Attempt 6: 64s
 * - Attempt 7: 128s
 * - Attempt 8: 256s
 * - Attempt 9: 512s
 * - Attempt 10: 1024s
 * - Attempt 11+: 3600s (max cap)
 * </pre>
 * </p>
 *
 * <p>
 * <strong>Jitter</strong>:
 * Adiciona variação aleatória (0-25%) para evitar thundering herd:
 * 
 * <pre>
 * finalDelay = delay * (1 + random(0, jitterFactor))
 * </pre>
 * </p>
 *
 * <p>
 * <strong>Benefícios</strong>:
 * <ul>
 * <li>Reduz carga em serviços downstream durante falhas</li>
 * <li>Jitter previne picos de retry simultâneos</li>
 * <li>Max delay evita esperas excessivas</li>
 * <li>Max attempts evita loops infinitos</li>
 * </ul>
 * </p>
 *
 * @since 1.2.0
 */
@Slf4j
@Getter
@Component
public class ExponentialBackoffRetryStrategy implements OutboxRetryStrategy {

    private final int maxAttempts;
    private final long baseDelaySeconds;
    private final long maxDelaySeconds;
    private final double jitterFactor;

    /**
     * Construtor com configurações injetadas do application.yml.
     *
     * @param maxAttempts      Máximo de tentativas (default: 10)
     * @param baseDelaySeconds Delay base em segundos (default: 1)
     * @param maxDelaySeconds  Delay máximo em segundos (default: 3600 = 1h)
     * @param jitterFactor     Fator de jitter 0-1 (default: 0.25 = 25%)
     */
    public ExponentialBackoffRetryStrategy(
            @Value("${outbox.retry.max-attempts:10}") int maxAttempts,
            @Value("${outbox.retry.base-delay-seconds:1}") long baseDelaySeconds,
            @Value("${outbox.retry.max-delay-seconds:3600}") long maxDelaySeconds,
            @Value("${outbox.retry.jitter-factor:0.25}") double jitterFactor) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be > 0");
        }
        if (baseDelaySeconds <= 0) {
            throw new IllegalArgumentException("baseDelaySeconds must be > 0");
        }
        if (maxDelaySeconds < baseDelaySeconds) {
            throw new IllegalArgumentException("maxDelaySeconds must be >= baseDelaySeconds");
        }
        if (jitterFactor < 0 || jitterFactor > 1) {
            throw new IllegalArgumentException("jitterFactor must be between 0 and 1");
        }

        this.maxAttempts = maxAttempts;
        this.baseDelaySeconds = baseDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
        this.jitterFactor = jitterFactor;

        log.info("ExponentialBackoffRetryStrategy configured: maxAttempts={}, baseDelay={}s, " +
                "maxDelay={}s, jitter={}%",
                maxAttempts, baseDelaySeconds, maxDelaySeconds, jitterFactor * 100);
    }

    @Override
    public Instant calculateNextRetryTime(int currentAttempts) {
        if (currentAttempts < 0) {
            throw new IllegalArgumentException("currentAttempts must be >= 0");
        }

        // 1. Calcular delay exponencial: baseDelay * 2^attempts
        // Limitado a 30 para evitar overflow (2^30 = 1 bilhão de segundos = 31 anos)
        long delaySeconds = (long) (baseDelaySeconds * Math.pow(2, Math.min(currentAttempts, 30)));

        // 2. Aplicar cap máximo
        delaySeconds = Math.min(delaySeconds, maxDelaySeconds);

        // 3. Aplicar jitter (variação aleatória 0 a jitterFactor%)
        // Se jitterFactor=0, não aplicar jitter (evita nextDouble(0) que lança exceção)
        long finalDelaySeconds = delaySeconds;
        if (jitterFactor > 0) {
            double jitter = 1.0 + ThreadLocalRandom.current().nextDouble(jitterFactor);
            finalDelaySeconds = (long) (delaySeconds * jitter);

            // 4. Garantir que não ultrapassou o máximo após jitter
            finalDelaySeconds = Math.min(finalDelaySeconds, maxDelaySeconds);

            log.debug("Calculated retry delay for attempt {}: {}s (base: {}s, jitter: {:.2f})",
                    currentAttempts, finalDelaySeconds, delaySeconds, jitter);
        } else {
            log.debug("Calculated retry delay for attempt {}: {}s (no jitter)",
                    currentAttempts, finalDelaySeconds);
        }

        return Instant.now().plusSeconds(finalDelaySeconds);
    }

    @Override
    public boolean canRetry(int attempts) {
        return attempts < maxAttempts;
    }
}
