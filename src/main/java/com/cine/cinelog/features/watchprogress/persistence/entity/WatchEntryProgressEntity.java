package com.cine.cinelog.features.watchprogress.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA para progresso de visualização de séries.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * <p>
 * Mapeia tabela `watch_entry_progress` com relacionamento 1:1 para
 * `watch_entry`.
 * Armazena progresso por episódio (season, episode, duração assistida).
 *
 * @since 1.0 (PR6 - Fase 5)
 */
@Entity
@Table(name = "watch_entry_progress")
public class WatchEntryProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "watch_entry_id", nullable = false, unique = true)
    private Long watchEntryId;

    @Column(name = "current_season", nullable = false)
    private Integer currentSeason;

    @Column(name = "current_episode", nullable = false)
    private Integer currentEpisode;

    @Column(name = "watched_duration_seconds", nullable = false)
    private Long watchedDurationSeconds;

    @Column(name = "total_duration_seconds", nullable = false)
    private Long totalDurationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Getters/Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWatchEntryId() {
        return watchEntryId;
    }

    public void setWatchEntryId(Long watchEntryId) {
        this.watchEntryId = watchEntryId;
    }

    public Integer getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(Integer currentSeason) {
        this.currentSeason = currentSeason;
    }

    public Integer getCurrentEpisode() {
        return currentEpisode;
    }

    public void setCurrentEpisode(Integer currentEpisode) {
        this.currentEpisode = currentEpisode;
    }

    public Long getWatchedDurationSeconds() {
        return watchedDurationSeconds;
    }

    public void setWatchedDurationSeconds(Long watchedDurationSeconds) {
        this.watchedDurationSeconds = watchedDurationSeconds;
    }

    public Long getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Long totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
