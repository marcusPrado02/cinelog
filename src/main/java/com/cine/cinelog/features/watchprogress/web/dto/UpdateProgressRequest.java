package com.cine.cinelog.features.watchprogress.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de requisição para atualizar progresso de visualização.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * @param currentSeason          temporada atual (≥ 1)
 * @param currentEpisode         episódio atual (≥ 1)
 * @param watchedDurationSeconds tempo assistido em segundos (≥ 0)
 * @param totalDurationSeconds   duração total do episódio em segundos (> 0)
 *
 * @since 1.0 (PR6 - Fase 5)
 */
@Schema(description = "Requisição para atualizar progresso de visualização de série")
public record UpdateProgressRequest(
        @Schema(description = "Temporada atual", example = "2", minimum = "1") int currentSeason,

        @Schema(description = "Episódio atual", example = "5", minimum = "1") int currentEpisode,

        @Schema(description = "Tempo assistido em segundos", example = "930", minimum = "0") long watchedDurationSeconds,

        @Schema(description = "Duração total do episódio em segundos", example = "2700", minimum = "1") long totalDurationSeconds) {
}
