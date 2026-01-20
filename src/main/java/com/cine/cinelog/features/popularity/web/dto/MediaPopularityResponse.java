package com.cine.cinelog.features.popularity.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta contendo métricas de popularidade de uma mídia.
 *
 * <p>
 * Retorna estatísticas agregadas atualizadas de forma assíncrona via eventos
 * Kafka.
 * <strong>Eventual consistency:</strong> pode haver delay entre visualização e
 * atualização.
 *
 * @param mediaId       ID da mídia
 * @param watchCount    Total de visualizações
 * @param ratingsCount  Total de avaliações
 * @param avgRating     Média de rating (0-10)
 * @param lastWatchedAt Data/hora da última visualização
 * @param updatedAt     Última atualização das métricas
 *
 * @since 1.0 (PR6)
 */
@Schema(name = "MediaPopularityResponse", description = "Métricas de popularidade de uma mídia")
public record MediaPopularityResponse(
        @Schema(description = "ID da mídia", example = "456") Long mediaId,

        @Schema(description = "Total de visualizações", example = "1250") Long watchCount,

        @Schema(description = "Total de avaliações", example = "342") Long ratingsCount,

        @Schema(description = "Média de rating (0-10)", example = "8.7") BigDecimal avgRating,

        @Schema(description = "Data/hora da última visualização", example = "2026-01-11T14:25:00") LocalDateTime lastWatchedAt,

        @Schema(description = "Última atualização das métricas", example = "2026-01-11T14:30:00") LocalDateTime updatedAt) {
}
