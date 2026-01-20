package com.cine.cinelog.features.media.integration.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de resposta da API TMDB contendo informações básicas de um filme.
 * 
 * <p>
 * Representa os dados retornados pela API do The Movie Database (TMDB) ao
 * buscar informações de filmes.
 * Utilizado para deserializar respostas JSON da API externa.
 * 
 * <p>
 * Campos principais:
 * <ul>
 * <li>id: identificador único do filme no TMDB</li>
 * <li>title: título do filme</li>
 * <li>originalTitle: título original</li>
 * <li>overview: sinopse</li>
 * <li>releaseDate: data de lançamento</li>
 * <li>runtime: duração em minutos</li>
 * <li>voteAverage: média de avaliações</li>
 * <li>genres: lista de gêneros do filme</li>
 * </ul>
 * 
 * @since 1.0
 */
public class TmdbMovieResponse {

    private Long id;

    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;

    @JsonProperty("release_date")
    private String releaseDate;

    private Integer runtime;

    @JsonProperty("vote_average")
    private Double voteAverage;

    private List<Genre> genres;

    public static class Genre {
        private Long id;
        private String name;

        // getters/setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public Double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(Double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }
}
