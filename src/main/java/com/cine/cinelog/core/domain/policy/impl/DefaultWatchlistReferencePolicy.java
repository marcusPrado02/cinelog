package com.cine.cinelog.core.domain.policy.impl;

import java.time.Year;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.core.domain.policy.WatchlistReferencePolicy;

/**
 * Regras WL8–WL10:
 *
 * WL8: mídia deve existir.
 * WL9: apenas tipos de mídia permitidos (MOVIE / SERIES).
 * WL10: ano de lançamento não pode ser muito distante no futuro.
 */
public class DefaultWatchlistReferencePolicy implements WatchlistReferencePolicy {

    private final MediaRepositoryPort mediaRepo;
    private final int futureSlackYears;

    public DefaultWatchlistReferencePolicy(MediaRepositoryPort mediaRepo, int futureSlackYears) {
        this.mediaRepo = mediaRepo;
        this.futureSlackYears = futureSlackYears;
    }

    public DefaultWatchlistReferencePolicy(MediaRepositoryPort mediaRepo) {
        this(mediaRepo, 5); // por exemplo, até 5 anos no futuro
    }

    @Override
    public void validateCreate(WatchlistItem item) {
        validateReferences(item);
    }

    private void validateReferences(WatchlistItem item) {
        if (item == null || item.getMediaId() == null) {
            return;
        }

        // WL8: mídia deve existir
        Media media = mediaRepo.findById(item.getMediaId())
                .orElseThrow(() -> DomainException.of(
                        ErrorCode.MEDIA_NOT_FOUND,
                        "Media not found for watchlist: " + item.getMediaId()));

        // WL9: tipo de mídia permitido
        if (!isAllowedType(media.getType())) {
            throw DomainException.of(
                    ErrorCode.WATCHLIST_MEDIA_TYPE_NOT_ALLOWED,
                    media.getType().name());
        }

        // WL10: ano de lançamento não muito distante no futuro
        if (media.getReleaseYear() != null) {
            int currentYear = Year.now().getValue();
            int maxYear = currentYear + futureSlackYears;

            if (media.getReleaseYear() > maxYear) {
                throw DomainException.of(
                        ErrorCode.WATCHLIST_MEDIA_RELEASE_YEAR_INVALID,
                        media.getReleaseYear(),
                        maxYear);
            }
        }
    }

    private boolean isAllowedType(MediaType type) {
        if (type == null) {
            return false;
        }
        return type == MediaType.MOVIE || type == MediaType.SERIES;
    }
}
