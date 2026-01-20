package com.cine.cinelog.features.watchlist.persistence;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.cine.cinelog.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
/**
 * Entidade JPA que representa a tabela de watchlistitem no banco de dados.
 * Mapeia a estrutura de persistência para WatchlistItem.
 * 
 * <p>Esta entidade contém as anotações JPA necessárias para mapeamento objeto-relacional
 * e é convertida para/de WatchlistItem pelo WatchlistItemMapper.</p>
 * 
 * @since 1.0
 * @see WatchlistItem
 */
@Table(name = "watchlist")
public class WatchlistItemEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long mediaId;

    @Column(nullable = false)
    private LocalDateTime addedAt;

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

}