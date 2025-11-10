package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchEntry {
    private Long id;
    private Long userId;
    private Long mediaId;
    private Long episodeId;
    private Integer rating;
    private String comment;
    private LocalDate watchedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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

    @Override
    public boolean equals(Object o) {
        return o instanceof WatchEntry we && Objects.equals(id, we.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void applyRating(Integer rating, String comment) {
        this.setRating(rating);
        if (comment != null && !comment.isBlank()) {
            this.setComment(comment.trim());
        }
        this.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
    }

}