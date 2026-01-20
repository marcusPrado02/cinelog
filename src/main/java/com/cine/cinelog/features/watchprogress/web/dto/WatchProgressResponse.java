package com.cine.cinelog.features.watchprogress.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para progresso de visualização.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * @param watchEntryId ID do watch entry
 * @param currentSeason temporada atual
 * @param currentEpisode episódio atual
 * @param watchedDurationSeconds tempo assistido em segundos
 * @param totalDurationSeconds duração total do episódio em segundos
 * @param completionPercentage percentual de conclusão (0.0 - 100.0)
 * @param isEpisodeCompleted se episódio foi completado (≥ 90%)
 * @param formattedProgress representação formatada (ex: "S02E05 - 15:30/45:00 (34%)")
 *
 * @since 1.0 (PR6 - Fase 5)
 */
@Schema(description = "Resposta com progresso de visualização de série")
public record WatchProgressResponse(
        @Schema(description = "ID do watch entry", example = "123")
        Long watchEntryId,

        @Schema(description = "Temporada atual", example = "2")
        int currentSeason,

        @Schema(description = "Episódio atual", example = "5")
        int currentEpisode,

        @Schema(description = "Tempo assistido em segundos", example = "930")
        long watchedDurationSeconds,

        @Schema(description = "Duração total do episódio em segundos", example = "2700")
        long totalDurationSeconds,

        @Schema(description = "Percentual de conclusão", example = "34.4")
        double completionPercentage,

        @Schema(description = "Episódio completado (≥ 90%)", example = "false")
        boolean isEpisodeCompleted,

        @Schema(description = "Progresso formatado", example = "S02E05 - 15:30/45:00 (34.4%)")
        String formattedProgress
) {
}
