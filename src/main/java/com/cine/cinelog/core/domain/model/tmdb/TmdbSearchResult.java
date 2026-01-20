package com.cine.cinelog.core.domain.model.tmdb;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado genérico de busca/discover no TMDb.
 *
 * Representa uma página de resultados contendo itens de tipo T.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbSearchResult<T> {

    private int page;
    private int totalPages;
    private int totalResults;

    private List<T> results;

    public boolean isEmpty() {
        return results == null || results.isEmpty();
    }
}
