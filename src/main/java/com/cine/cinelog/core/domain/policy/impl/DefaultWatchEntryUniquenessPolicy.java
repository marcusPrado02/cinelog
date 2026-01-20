package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.WatchEntryUniquenessPolicy;

/**
 * R5: evita duplicidade lógica de WatchEntry
 * (user, media, episode, watchedAt).
 */
@Component
public class DefaultWatchEntryUniquenessPolicy implements WatchEntryUniquenessPolicy {

    private final WatchEntryRepositoryPort repo;

    public DefaultWatchEntryUniquenessPolicy(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void validate(WatchEntry entry) {
        if (entry.getWatchedAt() == null) {
            // sem data não faz sentido checar duplicidade,
            // a R4 já vai barrar rating sem data.
            return;
        }

        boolean exists = repo.existsByUserMediaEpisodeAndDate(
                entry.getUserId(),
                entry.getMediaId(),
                entry.getEpisodeId(),
                entry.getWatchedAt());

        if (exists) {
            throw DomainException.of(
                    ErrorCode.WATCH_ENTRY_DUPLICATE,
                    entry.getUserId(),
                    entry.getMediaId(),
                    entry.getEpisodeId(),
                    entry.getWatchedAt());
        }
    }
}
