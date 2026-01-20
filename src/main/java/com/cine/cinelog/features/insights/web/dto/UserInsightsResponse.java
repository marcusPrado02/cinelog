package com.cine.cinelog.features.insights.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta contendo insights completos de um usuário.
 *
 * <p>
 * Retorna estatísticas agregadas atualizadas de forma assíncrona via eventos
 * Kafka.
 * <strong>Eventual consistency:</strong> pode haver delay entre ação e
 * atualização.
 *
 * @param userId        ID do usuário
 * @param totalWatched  Total de mídias assistidas (filmes + séries)
 * @param totalMovies   Total de filmes assistidos
 * @param totalSeries   Total de séries assistidas
 * @param avgRating     Média de rating do usuário (0-10)
 * @param lastWatchedAt Data da última visualização
 * @param updatedAt     Última atualização das estatísticas
 *
 * @since 1.0 (PR6)
 */
@Schema(name = "UserInsightsResponse", description = "Estatísticas agregadas de visualização do usuário")
public record UserInsightsResponse(
        @Schema(description = "ID do usuário", example = "123") Long userId,

        @Schema(description = "Total de mídias assistidas (filmes + séries)", example = "42") Long totalWatched,

        @Schema(description = "Total de filmes assistidos", example = "25") Long totalMovies,

        @Schema(description = "Total de séries assistidas", example = "17") Long totalSeries,

        @Schema(description = "Média de rating do usuário (0-10)", example = "8.5") BigDecimal avgRating,

        @Schema(description = "Data da última visualização", example = "2026-01-10") LocalDate lastWatchedAt,

        @Schema(description = "Última atualização das estatísticas", example = "2026-01-11T14:30:00") LocalDateTime updatedAt) {
}
