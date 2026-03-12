package com.cine.cinelog.features.reports.data;

import java.math.BigDecimal;

/**
 * A lightweight representation of a media item used in report data.
 */
public class MediaItem {

    private Long id;
    private String title;
    private String type; // MOVIE | TV_SHOW
    private String posterUrl;
    private BigDecimal avgRating;
    private Long watchCount;
    private Integer releaseYear;

    public MediaItem() {
    }

    public MediaItem(Long id, String title, String type, String posterUrl,
            BigDecimal avgRating, Long watchCount, Integer releaseYear) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.posterUrl = posterUrl;
        this.avgRating = avgRating;
        this.watchCount = watchCount;
        this.releaseYear = releaseYear;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Long getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(Long watchCount) {
        this.watchCount = watchCount;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }
}
