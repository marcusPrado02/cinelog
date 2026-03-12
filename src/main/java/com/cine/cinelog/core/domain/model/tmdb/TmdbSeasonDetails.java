package com.cine.cinelog.core.domain.model.tmdb;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detalhes de uma temporada de série retornada pelo TMDb.
 * Mapeia a resposta de GET /tv/{id}/season/{season_number}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbSeasonDetails {

    /** Número da temporada (1-based). */
    private Integer seasonNumber;

    /** Nome da temporada (ex: "Season 1", "Especiais"). */
    private String name;

    /** Data de exibição da primeira temporada. */
    private LocalDate airDate;

    /** Lista de episódios desta temporada. */
    private List<TmdbEpisodeDetails> episodes;
}
