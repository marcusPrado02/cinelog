package com.cine.cinelog.features.reports.data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data for the admin platform report.
 */
public class PlatformReportData {

    private Long totalUsers;
    private Long totalMedia;
    private Long totalWatchEntries;
    private Long newUsersThisWeek;
    private Long newMediaThisWeek;
    private Long newWatchEntriesThisWeek;

    private List<MediaItem> topRatedMedia;
    private List<MediaItem> trendingMedia;

    private LocalDateTime generatedAt;

    public PlatformReportData() {
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalMedia() {
        return totalMedia;
    }

    public void setTotalMedia(Long totalMedia) {
        this.totalMedia = totalMedia;
    }

    public Long getTotalWatchEntries() {
        return totalWatchEntries;
    }

    public void setTotalWatchEntries(Long totalWatchEntries) {
        this.totalWatchEntries = totalWatchEntries;
    }

    public Long getNewUsersThisWeek() {
        return newUsersThisWeek;
    }

    public void setNewUsersThisWeek(Long newUsersThisWeek) {
        this.newUsersThisWeek = newUsersThisWeek;
    }

    public Long getNewMediaThisWeek() {
        return newMediaThisWeek;
    }

    public void setNewMediaThisWeek(Long newMediaThisWeek) {
        this.newMediaThisWeek = newMediaThisWeek;
    }

    public Long getNewWatchEntriesThisWeek() {
        return newWatchEntriesThisWeek;
    }

    public void setNewWatchEntriesThisWeek(Long newWatchEntriesThisWeek) {
        this.newWatchEntriesThisWeek = newWatchEntriesThisWeek;
    }

    public List<MediaItem> getTopRatedMedia() {
        return topRatedMedia;
    }

    public void setTopRatedMedia(List<MediaItem> topRatedMedia) {
        this.topRatedMedia = topRatedMedia;
    }

    public List<MediaItem> getTrendingMedia() {
        return trendingMedia;
    }

    public void setTrendingMedia(List<MediaItem> trendingMedia) {
        this.trendingMedia = trendingMedia;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
