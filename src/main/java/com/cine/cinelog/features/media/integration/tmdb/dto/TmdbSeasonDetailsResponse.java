package com.cine.cinelog.features.media.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO para resposta de GET /tv/{tv_id}/season/{season_number}.
 */
public class TmdbSeasonDetailsResponse {

    @JsonProperty("season_number")
    private Integer seasonNumber;

    private String name;

    @JsonProperty("air_date")
    private String airDate;

    private List<TmdbEpisodeDetailsResponse> episodes;

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
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

    public List<TmdbEpisodeDetailsResponse> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<TmdbEpisodeDetailsResponse> episodes) {
        this.episodes = episodes;
    }
}
