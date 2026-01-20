package com.cine.cinelog.infrastructure.persistence.outbox.retry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para ExponentialBackoffRetryStrategy.
 *
 * <p>
 * Valida:
 * <ul>
 * <li>Exponential backoff: delay dobra a cada tentativa</li>
 * <li>Jitter: variação aleatória aplicada corretamente</li>
 * <li>Max delay cap: delay não ultrapassa máximo</li>
 * <li>Max attempts: retry para após máximo de tentativas</li>
 * <li>Validações de parâmetros</li>
 * </ul>
 * </p>
 */
@DisplayName("ExponentialBackoffRetryStrategy")
class ExponentialBackoffRetryStrategyTest {

    private ExponentialBackoffRetryStrategy strategy;

    @BeforeEach
    void setUp() {
        // Configuração padrão: max=10 attempts, base=1s, max=3600s, jitter=25%
        strategy = new ExponentialBackoffRetryStrategy(10, 1, 3600, 0.25);
    }

    @Nested
    @DisplayName("Construtor")
    class ConstructorTests {

        @Test
        @DisplayName("Deve aceitar parâmetros válidos")
        void shouldAcceptValidParameters() {
            assertThatCode(() -> new ExponentialBackoffRetryStrategy(5, 2, 7200, 0.5))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve rejeitar maxAttempts <= 0")
        void shouldRejectInvalidMaxAttempts() {
            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(0, 1, 3600, 0.25))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxAttempts must be > 0");

            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(-1, 1, 3600, 0.25))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve rejeitar baseDelaySeconds <= 0")
        void shouldRejectInvalidBaseDelay() {
            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(10, 0, 3600, 0.25))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("baseDelaySeconds must be > 0");

            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(10, -1, 3600, 0.25))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve rejeitar maxDelaySeconds < baseDelaySeconds")
        void shouldRejectInvalidMaxDelay() {
            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(10, 10, 5, 0.25))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxDelaySeconds must be >= baseDelaySeconds");
        }

        @Test
        @DisplayName("Deve rejeitar jitterFactor fora de [0, 1]")
        void shouldRejectInvalidJitterFactor() {
            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(10, 1, 3600, -0.1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jitterFactor must be between 0 and 1");

            assertThatThrownBy(() -> new ExponentialBackoffRetryStrategy(10, 1, 3600, 1.5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve aceitar jitterFactor = 0 (sem jitter)")
        void shouldAcceptZeroJitter() {
            assertThatCode(() -> new ExponentialBackoffRetryStrategy(10, 1, 3600, 0.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar jitterFactor = 1 (100% jitter)")
        void shouldAcceptMaxJitter() {
            assertThatCode(() -> new ExponentialBackoffRetryStrategy(10, 1, 3600, 1.0))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("calculateNextRetryTime()")
    class CalculateNextRetryTimeTests {

        @Test
        @DisplayName("Deve rejeitar currentAttempts negativo")
        void shouldRejectNegativeAttempts() {
            assertThatThrownBy(() -> strategy.calculateNextRetryTime(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentAttempts must be >= 0");
        }

        @Test
        @DisplayName("Attempt 0: delay ~1s (com jitter até 1.25s)")
        void shouldCalculateDelayForAttempt0() {
            Instant now = Instant.now();
            Instant nextRetry = strategy.calculateNextRetryTime(0);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // baseDelay=1s, jitter=25% → delay entre 1s e 1.25s
            assertThat(delaySeconds).isBetween(1L, 2L);
        }

        @Test
        @DisplayName("Attempt 1: delay ~2s (com jitter até 2.5s)")
        void shouldCalculateDelayForAttempt1() {
            Instant now = Instant.now();
            Instant nextRetry = strategy.calculateNextRetryTime(1);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // baseDelay=1s * 2^1 = 2s, jitter=25% → delay entre 2s e 2.5s
            assertThat(delaySeconds).isBetween(2L, 3L);
        }

        @Test
        @DisplayName("Attempt 3: delay ~8s (com jitter até 10s)")
        void shouldCalculateDelayForAttempt3() {
            Instant now = Instant.now();
            Instant nextRetry = strategy.calculateNextRetryTime(3);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // baseDelay=1s * 2^3 = 8s, jitter=25% → delay entre 8s e 10s
            assertThat(delaySeconds).isBetween(8L, 11L);
        }

        @Test
        @DisplayName("Attempt 10: delay ~1024s (com jitter até 1280s)")
        void shouldCalculateDelayForAttempt10() {
            Instant now = Instant.now();
            Instant nextRetry = strategy.calculateNextRetryTime(10);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // baseDelay=1s * 2^10 = 1024s, jitter=25% → delay entre 1024s e 1280s
            assertThat(delaySeconds).isBetween(1024L, 1281L);
        }

        @Test
        @DisplayName("Attempt 12: delay cap no max (3600s)")
        void shouldApplyMaxDelayCap() {
            Instant now = Instant.now();
            Instant nextRetry = strategy.calculateNextRetryTime(12);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // baseDelay=1s * 2^12 = 4096s > maxDelay=3600s
            // Cap aplicado: 3600s (com jitter não ultrapassa max)
            assertThat(delaySeconds).isLessThanOrEqualTo(3600L);
        }

        @Test
        @DisplayName("Delay nunca ultrapassa maxDelay mesmo com jitter")
        void shouldNeverExceedMaxDelay() {
            // Testar 100 vezes para garantir que jitter aleatório não quebra o cap
            for (int i = 0; i < 100; i++) {
                Instant now = Instant.now();
                Instant nextRetry = strategy.calculateNextRetryTime(20); // Muito acima do cap

                long delaySeconds = Duration.between(now, nextRetry).getSeconds();
                assertThat(delaySeconds).isLessThanOrEqualTo(3600L);
            }
        }

        @Test
        @DisplayName("Jitter=0: delay exato sem variação")
        void shouldCalculateExactDelayWithoutJitter() {
            var noJitterStrategy = new ExponentialBackoffRetryStrategy(10, 1, 3600, 0.0);

            Instant now = Instant.now();
            Instant nextRetry = noJitterStrategy.calculateNextRetryTime(3);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // baseDelay=1s * 2^3 = 8s, sem jitter
            assertThat(delaySeconds).isEqualTo(8L);
        }

        @Test
        @DisplayName("Deve calcular delay para attempt muito grande sem overflow")
        void shouldHandleLargeAttemptsWithoutOverflow() {
            assertThatCode(() -> strategy.calculateNextRetryTime(100))
                    .doesNotThrowAnyException();

            Instant now = Instant.now();
            Instant nextRetry = strategy.calculateNextRetryTime(100);

            long delaySeconds = Duration.between(now, nextRetry).getSeconds();
            // Deve ser cappado no maxDelay=3600s
            assertThat(delaySeconds).isLessThanOrEqualTo(3600L);
        }
    }

    @Nested
    @DisplayName("canRetry()")
    class CanRetryTests {

        @Test
        @DisplayName("Deve permitir retry quando attempts < maxAttempts")
        void shouldAllowRetryWhenBelowMax() {
            assertThat(strategy.canRetry(0)).isTrue();
            assertThat(strategy.canRetry(5)).isTrue();
            assertThat(strategy.canRetry(9)).isTrue();
        }

        @Test
        @DisplayName("Deve rejeitar retry quando attempts >= maxAttempts")
        void shouldRejectRetryWhenAtOrAboveMax() {
            assertThat(strategy.canRetry(10)).isFalse();
            assertThat(strategy.canRetry(11)).isFalse();
            assertThat(strategy.canRetry(100)).isFalse();
        }

        @Test
        @DisplayName("Deve permitir retry exatamente em maxAttempts - 1")
        void shouldAllowRetryAtMaxMinusOne() {
            assertThat(strategy.canRetry(9)).isTrue();
        }
    }

    @Nested
    @DisplayName("Getters")
    class GetterTests {

        @Test
        @DisplayName("Deve retornar maxAttempts correto")
        void shouldReturnMaxAttempts() {
            assertThat(strategy.getMaxAttempts()).isEqualTo(10);
        }

        @Test
        @DisplayName("Deve retornar baseDelaySeconds correto")
        void shouldReturnBaseDelaySeconds() {
            assertThat(strategy.getBaseDelaySeconds()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve retornar maxDelaySeconds correto")
        void shouldReturnMaxDelaySeconds() {
            assertThat(strategy.getMaxDelaySeconds()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("Deve retornar jitterFactor correto")
        void shouldReturnJitterFactor() {
            assertThat(strategy.getJitterFactor()).isEqualTo(0.25);
        }
    }

    @Nested
    @DisplayName("Cenários de Integração")
    class IntegrationScenarios {

        @Test
        @DisplayName("Cenário: falha transitória recupera rapidamente")
        void transientFailureScenario() {
            // Attempt 0: ~1s → recupera logo
            Instant now = Instant.now();
            Instant retry0 = strategy.calculateNextRetryTime(0);

            long delaySeconds = Duration.between(now, retry0).getSeconds();
            // baseDelay=1s, jitter=25% → delay entre 1s e 1.25s
            // Tolerância maior para timing issues (0-3s)
            assertThat(delaySeconds).isBetween(0L, 3L);
        }

        @Test
        @DisplayName("Cenário: falha persistente escala delay")
        void persistentFailureScenario() {
            // Attempt 0: ~1s
            Instant retry0 = strategy.calculateNextRetryTime(0);
            // Attempt 5: ~32s
            Instant retry5 = strategy.calculateNextRetryTime(5);
            // Attempt 10: ~1024s
            Instant retry10 = strategy.calculateNextRetryTime(10);

            Instant now = Instant.now();
            assertThat(Duration.between(now, retry0).getSeconds()).isLessThan(10L);
            assertThat(Duration.between(now, retry5).getSeconds()).isBetween(32L, 41L);
            assertThat(Duration.between(now, retry10).getSeconds()).isBetween(1024L, 1281L);
        }

        @Test
        @DisplayName("Cenário: após maxAttempts deve ir para DLQ")
        void shouldGoToDLQAfterMaxAttempts() {
            assertThat(strategy.canRetry(9)).isTrue(); // Última tentativa válida
            assertThat(strategy.canRetry(10)).isFalse(); // Deve ir para DLQ
        }
    }
}
