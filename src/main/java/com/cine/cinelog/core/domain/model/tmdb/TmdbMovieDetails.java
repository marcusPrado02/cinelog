package com.cine.cinelog.core.domain.model.tmdb;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmdbMovieDetails {

    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private LocalDate releaseDate;
    private Integer runtime;
    private Double voteAverage;
    private List<String> genres;

}
