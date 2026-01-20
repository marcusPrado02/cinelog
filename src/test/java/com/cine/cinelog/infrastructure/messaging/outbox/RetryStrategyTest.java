package com.cine.cinelog.infrastructure.messaging.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Testes unitários para RetryStrategy.
 */
@DisplayName("RetryStrategy")
class RetryStrategyTest {

    @Test
    @DisplayName("Deve calcular retry para primeira tentativa")
    void shouldCalculateRetryForFirstAttempt() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .maxDelaySeconds(3600)
                .maxAttempts(5)
                .jitterPercent(0) // sem jitter para teste determinístico
                .build();

        // Act
        RetryStrategy.RetryDecision decision = strategy.calculateNextRetry(0);

        // Assert
        assertThat(decision.isShouldRetry()).isTrue();
        assertThat(decision.getNextAttempt()).isEqualTo(1);
        assertThat(decision.getDelaySeconds()).isCloseTo(60L, within(10L)); // 60s ± jitter
        assertThat(decision.getNextRetryAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Deve aplicar backoff exponencial")
    void shouldApplyExponentialBackoff() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .maxDelaySeconds(3600)
                .maxAttempts(10)
                .jitterPercent(0)
                .build();

        // Act & Assert
        // Tentativa 0 → delay = 60 * 2^0 = 60s
        var decision0 = strategy.calculateNextRetry(0);
        assertThat(decision0.getDelaySeconds()).isEqualTo(60);

        // Tentativa 1 → delay = 60 * 2^1 = 120s
        var decision1 = strategy.calculateNextRetry(1);
        assertThat(decision1.getDelaySeconds()).isEqualTo(120);

        // Tentativa 2 → delay = 60 * 2^2 = 240s
        var decision2 = strategy.calculateNextRetry(2);
        assertThat(decision2.getDelaySeconds()).isEqualTo(240);

        // Tentativa 3 → delay = 60 * 2^3 = 480s
        var decision3 = strategy.calculateNextRetry(3);
        assertThat(decision3.getDelaySeconds()).isEqualTo(480);
    }

    @Test
    @DisplayName("Deve respeitar delay máximo (cap)")
    void shouldRespectMaxDelay() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .maxDelaySeconds(600) // 10 minutos max
                .maxAttempts(10)
                .jitterPercent(0)
                .build();

        // Act
        // Tentativa 10 → delay = 60 * 2^10 = 61440s, mas cap em 600s
        var decision = strategy.calculateNextRetry(10);

        // Assert
        assertThat(decision.getDelaySeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("Deve marcar como FAILED_PERM ao atingir max attempts")
    void shouldMarkAsFailedPermWhenMaxAttemptsReached() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .maxDelaySeconds(3600)
                .maxAttempts(5)
                .build();

        // Act - tentativa 4 (5ª tentativa, que é o máximo)
        var decision = strategy.calculateNextRetry(4);

        // Assert
        assertThat(decision.isShouldRetry()).isFalse();
        assertThat(decision.getNextRetryAt()).isNull();
        assertThat(decision.getReason()).contains("Max attempts");
        assertThat(decision.getReason()).contains("FAILED_PERM");
    }

    @Test
    @DisplayName("Deve aplicar jitter para evitar thundering herd")
    void shouldApplyJitterToAvoidThunderingHerd() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .maxDelaySeconds(3600)
                .maxAttempts(5)
                .jitterPercent(10.0) // ±10%
                .build();

        // Act - calcular 10 vezes e verificar que há variação
        long[] delays = new long[10];
        for (int i = 0; i < 10; i++) {
            var decision = strategy.calculateNextRetry(2); // tentativa 2
            delays[i] = decision.getDelaySeconds();
        }

        // Assert - deve ter pelo menos 3 valores diferentes (devido ao jitter)
        long distinctCount = java.util.Arrays.stream(delays).distinct().count();
        assertThat(distinctCount).isGreaterThanOrEqualTo(3);

        // Todos os delays devem estar no range esperado: 240s ± 10%
        for (long delay : delays) {
            assertThat(delay).isBetween(216L, 264L); // 240 * 0.9 a 240 * 1.1
        }
    }

    @ParameterizedTest
    @CsvSource({
            "0, 60", // 60 * 2^0
            "1, 120", // 60 * 2^1
            "2, 240", // 60 * 2^2
            "3, 480", // 60 * 2^3
            "4, 960", // 60 * 2^4
            "5, 1920", // 60 * 2^5
            "6, 3600" // 60 * 2^6 = 3840, mas cap em 3600
    })
    @DisplayName("Deve estimar delays corretamente para diferentes tentativas")
    void shouldEstimateDelaysCorrectly(int attemptNumber, long expectedDelay) {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .maxDelaySeconds(3600)
                .build();

        // Act
        long estimatedDelay = strategy.estimateDelayForAttempt(attemptNumber);

        // Assert
        assertThat(estimatedDelay).isEqualTo(expectedDelay);
    }

    @Test
    @DisplayName("Deve retornar 0 para tentativa 0 ou negativa ao estimar")
    void shouldReturn0ForAttempt0WhenEstimating() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder().build();

        // Act & Assert
        assertThat(strategy.estimateDelayForAttempt(0)).isEqualTo(0);
        assertThat(strategy.estimateDelayForAttempt(-1)).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve usar valores padrão quando não configurados")
    void shouldUseDefaultValues() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder().build();

        // Assert
        assertThat(strategy.getBaseDelaySeconds()).isEqualTo(60);
        assertThat(strategy.getMaxDelaySeconds()).isEqualTo(3600);
        assertThat(strategy.getMaxAttempts()).isEqualTo(5);
        assertThat(strategy.getJitterPercent()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Deve permitir configuração customizada")
    void shouldAllowCustomConfiguration() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(30)
                .maxDelaySeconds(1800)
                .maxAttempts(10)
                .jitterPercent(5.0)
                .build();

        // Assert
        assertThat(strategy.getBaseDelaySeconds()).isEqualTo(30);
        assertThat(strategy.getMaxDelaySeconds()).isEqualTo(1800);
        assertThat(strategy.getMaxAttempts()).isEqualTo(10);
        assertThat(strategy.getJitterPercent()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("NextRetryAt deve estar no futuro")
    void nextRetryAtShouldBeInFuture() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .baseDelaySeconds(60)
                .build();
        Instant before = Instant.now();

        // Act
        var decision = strategy.calculateNextRetry(0);

        // Assert
        assertThat(decision.getNextRetryAt()).isAfter(before);
        assertThat(decision.getNextRetryAt()).isBefore(Instant.now().plusSeconds(120));
    }

    @Test
    @DisplayName("Decisão de FAILED_PERM deve ter nextRetryAt null")
    void failedPermDecisionShouldHaveNullNextRetryAt() {
        // Arrange
        RetryStrategy strategy = RetryStrategy.builder()
                .maxAttempts(3)
                .build();

        // Act - tentativa 2 (3ª tentativa, que é o máximo)
        var decision = strategy.calculateNextRetry(2);

        // Assert
        assertThat(decision.isShouldRetry()).isFalse();
        assertThat(decision.getNextRetryAt()).isNull();
        assertThat(decision.getDelaySeconds()).isEqualTo(0);
    }
}
