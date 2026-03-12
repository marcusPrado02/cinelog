package com.cine.cinelog.core.domain.model.tmdb;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detalhes de um episódio dentro de uma temporada retornada pelo TMDb.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbEpisodeDetails {

    /** Número do episódio dentro da temporada. */
    private Integer episodeNumber;

    /** Título do episódio. */
    private String name;

    /** Data de exibição original do episódio. */
    private LocalDate airDate;
}
