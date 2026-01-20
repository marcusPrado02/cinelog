package com.cine.cinelog.core.domain.model.tmdb;

import java.util.List;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detalhes completos de uma mídia retornada pelo TMDb (filme ou série).
 *
 * Este modelo é usado para sincronizar/atualizar o catálogo local do CineLog
 * com informações oficiais do TMDb.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbMediaDetails {

    /**
     * Identificador único no TMDb.
     */
    private Long tmdbId;

    /**
     * Tipo da mídia no domínio local.
     */
    private MediaType type; // MOVIE / TV

    private String title;
    private String originalTitle;
    private String originalLanguage;

    /**
     * Sinopse/descrição da mídia.
     */
    private String overview;

    /**
     * Ano de lançamento (filme) ou primeira exibição (série).
     */
    private Integer releaseYear;

    /**
     * Avaliação média (0..10) no TMDb.
     */
    private Double voteAverage;

    /**
     * Duração em minutos (para filmes, e eventualmente para episódios médios).
     */
    private Integer runtimeMinutes;

    /**
     * URL absoluta do poster (já construída a partir de baseUrl + poster_path).
     */
    private String posterUrl;

    /**
     * URL absoluta do backdrop (já construída a partir de baseUrl + backdrop_path).
     */
    private String backdropUrl;

    /**
     * Lista de nomes de gêneros (ex.: ["Action", "Drama"]).
     */
    private List<String> genres;
}
