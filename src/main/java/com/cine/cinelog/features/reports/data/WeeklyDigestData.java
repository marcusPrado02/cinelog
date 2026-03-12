package com.cine.cinelog.features.reports.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data for the weekly digest email report.
 */
public class WeeklyDigestData {

    private Long userId;
    private String userName;
    private String userEmail;

    private Long totalWatchedThisWeek;
    private Long totalMoviesThisWeek;
    private Long totalSeriesThisWeek;

    private BigDecimal avgRatingOverall;
    private LocalDate weekStart;
    private LocalDate weekEnd;

    private List<MediaItem> recentlyWatched;
    private String favoriteGenre;

    public WeeklyDigestData() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getTotalWatchedThisWeek() {
        return totalWatchedThisWeek;
    }

    public void setTotalWatchedThisWeek(Long totalWatchedThisWeek) {
        this.totalWatchedThisWeek = totalWatchedThisWeek;
    }

    public Long getTotalMoviesThisWeek() {
        return totalMoviesThisWeek;
    }

    public void setTotalMoviesThisWeek(Long totalMoviesThisWeek) {
        this.totalMoviesThisWeek = totalMoviesThisWeek;
    }

    public Long getTotalSeriesThisWeek() {
        return totalSeriesThisWeek;
    }

    public void setTotalSeriesThisWeek(Long totalSeriesThisWeek) {
        this.totalSeriesThisWeek = totalSeriesThisWeek;
    }

    public BigDecimal getAvgRatingOverall() {
        return avgRatingOverall;
    }

    public void setAvgRatingOverall(BigDecimal avgRatingOverall) {
        this.avgRatingOverall = avgRatingOverall;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(LocalDate weekEnd) {
        this.weekEnd = weekEnd;
    }

    public List<MediaItem> getRecentlyWatched() {
        return recentlyWatched;
    }

    public void setRecentlyWatched(List<MediaItem> recentlyWatched) {
        this.recentlyWatched = recentlyWatched;
    }

    public String getFavoriteGenre() {
        return favoriteGenre;
    }

    public void setFavoriteGenre(String favoriteGenre) {
        this.favoriteGenre = favoriteGenre;
    }
}
