package com.cine.cinelog.features.reports.data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data for the "Top Actors" report — actors with the most highly-rated films.
 */
public class TopActorsData {

    private List<PersonItem> actors;
    private LocalDateTime generatedAt;
    private int limit;

    public TopActorsData() {
    }

    public TopActorsData(List<PersonItem> actors, LocalDateTime generatedAt, int limit) {
        this.actors = actors;
        this.generatedAt = generatedAt;
        this.limit = limit;
    }

    public List<PersonItem> getActors() {
        return actors;
    }

    public void setActors(List<PersonItem> actors) {
        this.actors = actors;
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
