package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.core.domain.policy.WatchlistUpdatePolicy;

/**
 * Implementa regras WL6 e WL7:
 *
 * WL6: userId é imutável.
 * WL7: mediaId é imutável.
 */
@Component
public class DefaultWatchlistUpdatePolicy implements WatchlistUpdatePolicy {

    @Override
    public void validate(WatchlistItem current, WatchlistItem updated) {
        if (current == null || updated == null) {
            return;
        }

        // WL6: userId imutável
        if (updated.getUserId() != null &&
                !updated.getUserId().equals(current.getUserId())) {
            throw DomainException.of(
                    ErrorCode.WATCHLIST_IMMUTABLE_USER,
                    "userId cannot be changed");
        }

        // WL7: mediaId imutável
        if (updated.getMediaId() != null &&
                !updated.getMediaId().equals(current.getMediaId())) {
            throw DomainException.of(
                    ErrorCode.WATCHLIST_IMMUTABLE_MEDIA,
                    "mediaId cannot be changed");
        }
    }
}
