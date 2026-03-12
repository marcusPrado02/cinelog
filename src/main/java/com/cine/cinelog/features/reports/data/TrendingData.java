package com.cine.cinelog.features.reports.data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data for the trending media email report (most watched in the last 7 days).
 */
public class TrendingData {

    private List<MediaItem> items;
    private LocalDateTime generatedAt;
    private int days;

    public TrendingData() {
    }

    public TrendingData(List<MediaItem> items, LocalDateTime generatedAt, int days) {
        this.items = items;
        this.generatedAt = generatedAt;
        this.days = days;
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
}
