package com.cine.cinelog.infrastructure.persistence.outbox.retry;

import java.time.Instant;

/**
 * Estratégia de retry para eventos Outbox falhados.
 *
 * <p>
 * Define quando e como eventos devem ser reprocessados após falhas.
 * Implementações comuns incluem:
 * <ul>
 * <li>Exponential Backoff: delay dobra a cada tentativa</li>
 * <li>Fixed Delay: delay fixo entre tentativas</li>
 * <li>Linear Backoff: delay aumenta linearmente</li>
 * </ul>
 * </p>
 *
 * @since 1.2.0
 */
public interface OutboxRetryStrategy {

    /**
     * Calcula o próximo instante de retry para um evento falhado.
     *
     * @param currentAttempts Número de tentativas já realizadas (>= 0)
     * @return Instant do próximo retry
     * @throws IllegalArgumentException se currentAttempts < 0
     */
    Instant calculateNextRetryTime(int currentAttempts);

    /**
     * Verifica se evento deve ser reprocessado ou enviado para DLQ.
     *
     * @param attempts Número de tentativas já realizadas
     * @return true se pode fazer retry, false se deve ir para DLQ
     */
    boolean canRetry(int attempts);

    /**
     * Retorna número máximo de tentativas permitidas.
     *
     * @return Máximo de tentativas antes de ir para DLQ
     */
    int getMaxAttempts();

    /**
     * Retorna delay base em segundos.
     *
     * @return Delay base usado no cálculo do backoff
     */
    long getBaseDelaySeconds();

    /**
     * Retorna delay máximo em segundos.
     *
     * @return Delay máximo permitido (cap para exponencial)
     */
    long getMaxDelaySeconds();
}
