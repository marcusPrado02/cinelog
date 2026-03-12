package com.cine.cinelog.features.reports.data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data for the top-rated media email report.
 */
public class TopRatedData {

    private List<MediaItem> items;
    private LocalDateTime generatedAt;
    private int limit;

    public TopRatedData() {
    }

    public TopRatedData(List<MediaItem> items, LocalDateTime generatedAt, int limit) {
        this.items = items;
        this.generatedAt = generatedAt;
        this.limit = limit;
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

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
