package com.cine.cinelog.features.readmodels.persistence.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade JPA para tabela media_popularity (CQRS Read Model).
 *
 * <p>
 * Armazena métricas agregadas de popularidade por mídia,
 * atualizadas de forma assíncrona via eventos Kafka (eventual consistency).
 *
 * <p>
 * Esta é uma tabela de leitura otimizada para queries de:
 * <ul>
 * <li><strong>Top Rated:</strong> ORDER BY avg_rating DESC</li>
 * <li><strong>Trending:</strong> ORDER BY watch_count DESC, last_watched_at
 * DESC</li>
 * <li><strong>Most Watched:</strong> ORDER BY watch_count DESC</li>
 * </ul>
 *
 * <p>
 * <strong>Atualização:</strong> Consumidores Kafka atualizam esta tabela ao
 * processar:
 * <ul>
 * <li>WatchEntryCreatedEvent: incrementa watch_count, recalcula avg_rating</li>
 * <li>WatchEntryUpdatedEvent: recalcula avg_rating se rating mudou</li>
 * <li>WatchEntryDeletedEvent: decrementa watch_count, recalcula avg_rating</li>
 * </ul>
 *
 * @since 1.0 (PR6)
 * @see com.cine.cinelog.core.domain.model.WatchEntry
 */
@Entity
@Table(name = "media_popularity")
public class MediaPopularityEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "ratings_count", nullable = false)
    private Long ratingsCount = 0L;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "watch_count", nullable = false)
    private Long watchCount = 0L;

    @Column(name = "last_watched_at")
    private LocalDateTime lastWatchedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public MediaPopularityEntity() {
    }

    public MediaPopularityEntity(Long mediaId) {
        this.mediaId = mediaId;
        this.ratingsCount = 0L;
        this.watchCount = 0L;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public Long getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(Long ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Long getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(Long watchCount) {
        this.watchCount = watchCount;
    }

    public LocalDateTime getLastWatchedAt() {
        return lastWatchedAt;
    }

    public void setLastWatchedAt(LocalDateTime lastWatchedAt) {
        this.lastWatchedAt = lastWatchedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Incrementa o contador watch_count em 1.
     */
    public void incrementWatchCount() {
        this.watchCount++;
    }

    /**
     * Decrementa o contador watch_count em 1.
     */
    public void decrementWatchCount() {
        if (this.watchCount > 0) {
            this.watchCount--;
        }
    }

    /**
     * Incrementa o contador de ratings.
     */
    public void incrementRatingsCount() {
        this.ratingsCount++;
    }

    /**
     * Decrementa o contador de ratings.
     */
    public void decrementRatingsCount() {
        if (this.ratingsCount > 0) {
            this.ratingsCount--;
        }
    }

    @Override
    public String toString() {
        return "MediaPopularityEntity{" +
                "mediaId=" + mediaId +
                ", ratingsCount=" + ratingsCount +
                ", avgRating=" + avgRating +
                ", watchCount=" + watchCount +
                ", lastWatchedAt=" + lastWatchedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
