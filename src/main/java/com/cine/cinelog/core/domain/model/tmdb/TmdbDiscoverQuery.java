package com.cine.cinelog.core.domain.model.tmdb;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filtros de busca para os endpoints de discover do TMDb.
 *
 * É um modelo de domínio – a transformação para query params da API
 * será feita no adapter de infraestrutura.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbDiscoverQuery {

    /**
     * Tipo de mídia alvo (MOVIE/TV).
     * Não é usado diretamente no TMDb, mas ajuda o domínio a decidir
     * qual endpoint usar (/discover/movie ou /discover/tv).
     */
    private MediaType type;

    /**
     * Lista de IDs de gênero em formato de string, ex.: "28,12" (Action,
     * Adventure).
     */
    private String withGenres;

    /**
     * Campo de ordenação do TMDb, ex.: "popularity.desc", "vote_average.desc".
     */
    private String sortBy;

    /**
     * Ano de lançamento (filme) ou de primeira exibição (série).
     */
    private Integer year;

    /**
     * Página desejada (>= 1).
     */
    private Integer page;
}
