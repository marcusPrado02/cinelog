package com.cine.cinelog.features.reports.data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data for the "New Releases" report — recently added media to the catalog.
 */
public class NewReleasesData {

    private List<MediaItem> items;
    private LocalDateTime generatedAt;
    private int days;
    private int limit;
    private long totalNewMedia;

    public NewReleasesData() {
    }

    public NewReleasesData(List<MediaItem> items, LocalDateTime generatedAt,
            int days, int limit, long totalNewMedia) {
        this.items = items;
        this.generatedAt = generatedAt;
        this.days = days;
        this.limit = limit;
        this.totalNewMedia = totalNewMedia;
    }

    public List<MediaItem> getItems() {
        return items;
    }

    public void setItems(List<MediaItem> items) {
        this.items = items;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public long getTotalNewMedia() {
        return totalNewMedia;
    }

    public void setTotalNewMedia(long totalNewMedia) {
        this.totalNewMedia = totalNewMedia;
    }
}
