package com.cine.cinelog.features.media.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para cada episódio dentro de TmdbSeasonDetailsResponse.
 * Mapeia objetos do array "episodes" em GET /tv/{id}/season/{season_number}.
 */
public class TmdbEpisodeDetailsResponse {

    @JsonProperty("episode_number")
    private Integer episodeNumber;

    private String name;

    @JsonProperty("air_date")
    private String airDate;

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }
}
