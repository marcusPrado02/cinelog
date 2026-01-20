package com.cine.cinelog.core.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
/**
 * Representa WatchlistItem no domínio do sistema.
 * 
 * <p>Esta classe encapsula os conceitos e regras de negócio relacionados a watchlistitem.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.</p>
 * 
 * @since 1.0
 */
@Setter
public class WatchlistItem extends Auditable {

    private Long id;
    private Long userId;
    private Long mediaId;
    private LocalDateTime addedAt;

    public WatchlistItem(Long id, Long userId, Long mediaId, LocalDateTime addedAt) {
        this.id = id;
        this.userId = userId;
        this.mediaId = mediaId;
        this.addedAt = (addedAt != null ? addedAt : LocalDateTime.now(ZoneOffset.UTC));
    }

    public boolean belongsTo(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }
}