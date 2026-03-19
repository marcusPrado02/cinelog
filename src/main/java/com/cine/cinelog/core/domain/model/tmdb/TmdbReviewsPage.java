package com.cine.cinelog.core.domain.model.tmdb;

import java.util.List;

/**
 * Página de reviews TMDB para uma mídia específica.
 * Agregado construído a partir da resposta paginada da API TMDB.
 */
public class TmdbReviewsPage {

    private Long tmdbMediaId; // ID TMDB da mídia
    private int page;
    private int totalPages;
    private int totalResults;
    private List<TmdbReviewResult> results;

    public Long getTmdbMediaId() {
        return tmdbMediaId;
    }

    public void setTmdbMediaId(Long tmdbMediaId) {
        this.tmdbMediaId = tmdbMediaId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<TmdbReviewResult> getResults() {
        return results;
    }

    public void setResults(List<TmdbReviewResult> results) {
        this.results = results;
    }
}
