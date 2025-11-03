package com.cine.cinelog.features.seasons.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "seasons", uniqueConstraints = @UniqueConstraint(name = "uk_season_media_number", columnNames = {
        "media_id", "season_number" }))
public class SeasonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;
    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;
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

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long m) {
        this.mediaId = m;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer n) {
        this.seasonNumber = n;
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