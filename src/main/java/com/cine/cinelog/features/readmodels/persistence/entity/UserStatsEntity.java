package com.cine.cinelog.features.readmodels.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade JPA para tabela user_stats (CQRS Read Model).
 *
 * <p>
 * Armazena estatísticas agregadas de visualização por usuário,
 * atualizadas de forma assíncrona via eventos Kafka (eventual consistency).
 *
 * <p>
 * Esta é uma tabela de leitura otimizada, separada da fonte de verdade
 * (watch_entry),
 * implementando o padrão CQRS (Command Query Responsibility Segregation).
 *
 * <p>
 * <strong>Atualização:</strong> Consumidores Kafka atualizam esta tabela ao
 * processar:
 * <ul>
 * <li>WatchEntryCreatedEvent: incrementa total_watched, recalcula
 * avg_rating</li>
 * <li>WatchEntryUpdatedEvent: recalcula avg_rating se rating mudou</li>
 * <li>WatchEntryDeletedEvent: decrementa total_watched, recalcula
 * avg_rating</li>
 * </ul>
 *
 * @since 1.0 (PR6)
 * @see com.cine.cinelog.core.domain.model.WatchEntry
 */
@Entity
@Table(name = "user_stats")
public class UserStatsEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_watched", nullable = false)
    private Long totalWatched = 0L;

    @Column(name = "total_movies", nullable = false)
    private Long totalMovies = 0L;

    @Column(name = "total_series", nullable = false)
    private Long totalSeries = 0L;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "last_watched_at")
    private LocalDate lastWatchedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public UserStatsEntity() {
    }

    public UserStatsEntity(Long userId) {
        this.userId = userId;
        this.totalWatched = 0L;
        this.totalMovies = 0L;
        this.totalSeries = 0L;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTotalWatched() {
        return totalWatched;
    }

    public void setTotalWatched(Long totalWatched) {
        this.totalWatched = totalWatched;
    }

    public Long getTotalMovies() {
        return totalMovies;
    }

    public void setTotalMovies(Long totalMovies) {
        this.totalMovies = totalMovies;
    }

    public Long getTotalSeries() {
        return totalSeries;
    }

    public void setTotalSeries(Long totalSeries) {
        this.totalSeries = totalSeries;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public LocalDate getLastWatchedAt() {
        return lastWatchedAt;
    }

    public void setLastWatchedAt(LocalDate lastWatchedAt) {
        this.lastWatchedAt = lastWatchedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Incrementa o contador total_watched em 1.
     */
    public void incrementWatched() {
        this.totalWatched++;
    }

    /**
     * Decrementa o contador total_watched em 1.
     */
    public void decrementWatched() {
        if (this.totalWatched > 0) {
            this.totalWatched--;
        }
    }

    /**
     * Incrementa o contador de filmes.
     */
    public void incrementMovies() {
        this.totalMovies++;
    }

    /**
     * Incrementa o contador de séries.
     */
    public void incrementSeries() {
        this.totalSeries++;
    }

    /**
     * Decrementa o contador de filmes.
     */
    public void decrementMovies() {
        if (this.totalMovies > 0) {
            this.totalMovies--;
        }
    }

    /**
     * Decrementa o contador de séries.
     */
    public void decrementSeries() {
        if (this.totalSeries > 0) {
            this.totalSeries--;
        }
    }

    @Override
    public String toString() {
        return "UserStatsEntity{" +
                "userId=" + userId +
                ", totalWatched=" + totalWatched +
                ", totalMovies=" + totalMovies +
                ", totalSeries=" + totalSeries +
                ", avgRating=" + avgRating +
                ", lastWatchedAt=" + lastWatchedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
