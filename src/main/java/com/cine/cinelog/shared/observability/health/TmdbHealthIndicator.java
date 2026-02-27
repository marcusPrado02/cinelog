package com.cine.cinelog.shared.observability.health;

import java.util.Collections;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.cine.cinelog.shared.config.tmdb.TmdbProperties;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Health indicator para a integração com a API TMDb.
 *
 * <p>
 * <strong>A10:2025 — Mishandling of Exceptional Conditions</strong>
 * </p>
 *
 * <p>
 * Reporta o estado de saúde da integração com o TMDb baseando-se em duas
 * fontes complementares de informação:
 * <ul>
 * <li><strong>Circuit Breaker state</strong>: se o circuit breaker para a
 * instância "tmdb" estiver OPEN ou HALF_OPEN, o indicador reporta
 * {@code DOWN} ou {@code UNKNOWN} respectivamente.</li>
 * <li><strong>Probe leve</strong>: quando o circuit breaker está CLOSED,
 * faz uma chamada GET /configuration ao TMDb para verificar se a API
 * está acessível. Falha nessa chamada → {@code DOWN}.</li>
 * </ul>
 *
 * <p>
 * Os detalhes expostos incluem:
 * <ul>
 * <li>{@code circuitBreakerState} — CLOSED / OPEN / HALF_OPEN</li>
 * <li>{@code failureRate} — taxa de falha do circuit breaker</li>
 * <li>{@code baseUrl} — URL base configurada para o TMDb</li>
 * </ul>
 *
 * @see CircuitBreakerRegistry
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbHealthIndicator implements HealthIndicator {

    private static final String CB_INSTANCE = "tmdb";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final WebClient tmdbWebClient;
    private final TmdbProperties tmdbProperties;

    @Override
    public Health health() {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_INSTANCE);
            CircuitBreaker.State state = cb.getState();
            float failureRate = cb.getMetrics().getFailureRate();

            var details = Health.unknown()
                    .withDetail("circuitBreakerState", state.name())
                    .withDetail("failureRate", failureRate + "%")
                    .withDetail("baseUrl", tmdbProperties.getBaseUrl());

            return switch (state) {
                case OPEN -> details.down()
                        .withDetail("reason", "Circuit breaker OPEN — TMDb indisponível")
                        .build();
                case HALF_OPEN -> details.unknown()
                        .withDetail("reason", "Circuit breaker HALF_OPEN — testando recuperação")
                        .build();
                case CLOSED, FORCED_OPEN, DISABLED, METRICS_ONLY -> {
                    // Probe leve: GET /configuration (endpoint barato, sem dados sensíveis)
                    try {
                        tmdbWebClient.get()
                                .uri("/configuration")
                                .retrieve()
                                .toBodilessEntity()
                                .block(java.time.Duration.ofSeconds(3));
                        yield details.up().build();
                    } catch (WebClientResponseException ex) {
                        yield details.down()
                                .withDetail("reason", "TMDb retornou HTTP " + ex.getStatusCode())
                                .build();
                    } catch (Exception ex) {
                        yield details.down()
                                .withDetail("reason", "Probe falhou: " + ex.getMessage())
                                .build();
                    }
                }
            };
        } catch (Exception ex) {
            log.warn("Erro ao verificar saúde do TMDb", ex);
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
