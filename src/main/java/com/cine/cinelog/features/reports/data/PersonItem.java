package com.cine.cinelog.features.reports.data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a person (actor/director) in report context, with profile image.
 */
public class PersonItem {

    private Long id;
    private String name;
    private String profileUrl;
    private Long tmdbPersonId;
    private int filmCount;
    private BigDecimal avgRating;
    private String topRole; // ACTOR, DIRECTOR, etc.
    private List<MediaItem> topMedia; // up to 3 best-rated works

    public PersonItem() {
    }

    public PersonItem(Long id, String name, String profileUrl, Long tmdbPersonId,
            int filmCount, BigDecimal avgRating, String topRole) {
        this.id = id;
        this.name = name;
        this.profileUrl = profileUrl;
        this.tmdbPersonId = tmdbPersonId;
        this.filmCount = filmCount;
        this.avgRating = avgRating;
        this.topRole = topRole;
    }

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

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public Long getTmdbPersonId() {
        return tmdbPersonId;
    }

    public void setTmdbPersonId(Long tmdbPersonId) {
        this.tmdbPersonId = tmdbPersonId;
    }

    public int getFilmCount() {
        return filmCount;
    }

    public void setFilmCount(int filmCount) {
        this.filmCount = filmCount;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public String getTopRole() {
        return topRole;
    }

    public void setTopRole(String topRole) {
        this.topRole = topRole;
    }

    public List<MediaItem> getTopMedia() {
        return topMedia;
    }

    public void setTopMedia(List<MediaItem> topMedia) {
        this.topMedia = topMedia;
    }
}
