package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public class WatchEntry {
    private final Long id;
    private final Long userId;
    private final Long mediaId;
    private final Long episodeId;
    private final Integer rating;
    private final String comment;
    private final LocalDate watchedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public WatchEntry(Long id, Long userId, Long mediaId, Long episodeId,
            Integer rating, String comment, LocalDate watchedAt,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {

        if (userId == null)
            throw new IllegalArgumentException("userId is required");
        if (mediaId == null && episodeId == null)
            throw new IllegalArgumentException("Either mediaId or episodeId must be provided");
        if (rating != null && (rating < 0 || rating > 10))
            throw new IllegalArgumentException("rating must be 0..10");

        this.id = id;
        this.userId = userId;
        this.mediaId = mediaId;
        this.episodeId = episodeId;
        this.rating = rating;
        this.comment = comment;
        this.watchedAt = watchedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public WatchEntry withId(Long id) {
        return new WatchEntry(id, userId, mediaId, episodeId, rating, comment, watchedAt, createdAt, updatedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDate getWatchedAt() {
        return watchedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WatchEntry we && Objects.equals(id, we.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}