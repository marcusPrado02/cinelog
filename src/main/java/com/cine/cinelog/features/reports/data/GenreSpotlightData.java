package com.cine.cinelog.features.reports.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data for the "Genre Spotlight" report — deep dive on a popular genre.
 */
public class GenreSpotlightData {

    private String genreName;
    private long totalMedia;
    private long totalMovies;
    private long totalSeries;
    private BigDecimal avgRating;
    private List<MediaItem> topRatedInGenre;
    private List<PersonItem> topActorsInGenre;
    private LocalDateTime generatedAt;

    public GenreSpotlightData() {
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public long getTotalMedia() {
        return totalMedia;
    }

    public void setTotalMedia(long totalMedia) {
        this.totalMedia = totalMedia;
    }

    public long getTotalMovies() {
        return totalMovies;
    }

    public void setTotalMovies(long totalMovies) {
        this.totalMovies = totalMovies;
    }

    public long getTotalSeries() {
        return totalSeries;
    }

    public void setTotalSeries(long totalSeries) {
        this.totalSeries = totalSeries;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public List<MediaItem> getTopRatedInGenre() {
        return topRatedInGenre;
    }

    public void setTopRatedInGenre(List<MediaItem> topRatedInGenre) {
        this.topRatedInGenre = topRatedInGenre;
    }

    public List<PersonItem> getTopActorsInGenre() {
        return topActorsInGenre;
    }

    public void setTopActorsInGenre(List<PersonItem> topActorsInGenre) {
        this.topActorsInGenre = topActorsInGenre;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
