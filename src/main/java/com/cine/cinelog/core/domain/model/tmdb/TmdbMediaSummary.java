package com.cine.cinelog.core.domain.model.tmdb;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumo de mídia retornado em buscas (search/discover) do TMDb.
 *
 * Utilizado para exibir listas, sem precisar de todos os detalhes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbMediaSummary {

    private Long tmdbId;
    private MediaType type;

    private String title;
    private Integer releaseYear;

    private String overview;

    /**
     * Avaliação média (0..10).
     */
    private Double voteAverage;

    /**
     * URL absoluta do poster (já resolvida).
     */
    private String posterUrl;
}
