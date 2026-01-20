package com.cine.cinelog.features.media.integration.tmdb.dto;

import java.util.List;

/**
 * DTO para resposta de GET /genre/movie/list e /genre/tv/list.
 */
public class TmdbGenreListResponse {

    private List<Genre> genres;

    public static class Genre {
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }
}
