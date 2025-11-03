package com.cine.cinelog.features.episodes.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "episodes", uniqueConstraints = @UniqueConstraint(name = "uk_episode_season_number", columnNames = {
        "season_id", "episode_number" }))
public class EpisodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;
    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;
    @Column(length = 200)
    private String name;
    @Column(name = "air_date")
    private LocalDate airDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Long s) {
        this.seasonId = s;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer n) {
        this.episodeNumber = n;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public LocalDate getAirDate() {
        return airDate;
    }

    public void setAirDate(LocalDate d) {
        this.airDate = d;
    }
}