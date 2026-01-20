package com.cine.cinelog.features.watchentry.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.shared.persistence.AuditableEntity;

@Entity
@Table(name = "watch_entry", uniqueConstraints = @UniqueConstraint(name = "uk_we_user_media_episode_date", columnNames = {
        /**
         * Entidade JPA que representa a tabela de watchentry no banco de dados.
         * Mapeia a estrutura de persistência para WatchEntry.
         *
         * <p>
         * Esta entidade contém as anotações JPA necessárias para mapeamento
         * objeto-relacional
         * e é convertida para/de WatchEntry pelo WatchEntryMapper.
         * </p>
         *
         * @since 1.0
         * @see WatchEntry
         */
        "user_id", "media_id", "episode_id", "watched_at" }))
public class WatchEntryEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "episode_id")
    private Long episodeId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "watched_at")
    private LocalDate watchedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_type", nullable = false, length = 20)
    private WatchEntryStatusType statusType = WatchEntryStatusType.PLANNING;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (createdAt == null)
            createdAt = now;
        if (updatedAt == null)
            updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Long episodeId) {
        this.episodeId = episodeId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDate getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(LocalDate watchedAt) {
        this.watchedAt = watchedAt;
    }

    public WatchEntryStatusType getStatusType() {
        return statusType;
    }

    public void setStatusType(WatchEntryStatusType statusType) {
        this.statusType = statusType;
    }

}
