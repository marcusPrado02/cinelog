package com.cine.cinelog.core.domain.model.tmdb;

import java.time.OffsetDateTime;

/**
 * Representa uma review individual retornada pela API TMDB
 * (GET /movie/{id}/reviews ou GET /tv/{id}/reviews).
 */
public class TmdbReviewResult {

    private String id; // ID em string do TMDB (ex: "5b1c13b9c3a36848f2026384")
    private String author;
    private String authorUsername;
    private String authorAvatarPath;
    private Double authorRating; // nota do autor (0-10), pode ser null
    private String content;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorAvatarPath() {
        return authorAvatarPath;
    }

    public void setAuthorAvatarPath(String authorAvatarPath) {
        this.authorAvatarPath = authorAvatarPath;
    }

    public Double getAuthorRating() {
        return authorRating;
    }

    public void setAuthorRating(Double authorRating) {
        this.authorRating = authorRating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
