package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaTypeChangePolicy;
/**
 * Política de domínio para gerenciamento de defaultmediatypechange.
 * Define as regras e validações relacionadas a defaultmediatypechange.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultMediaTypeChange.</p>
 * 
 * @since 1.0
 * @see DefaultMediaTypeChange
 */

@Component
public class DefaultMediaTypeChangePolicy implements MediaTypeChangePolicy {

    private final WatchEntryRepositoryPort watchRepo;
    private final SeasonRepositoryPort seasonRepo;
    private final EpisodeRepositoryPort episodeRepo;

    public DefaultMediaTypeChangePolicy(
            WatchEntryRepositoryPort watchRepo,
            SeasonRepositoryPort seasonRepo,
            EpisodeRepositoryPort episodeRepo) {
        this.watchRepo = watchRepo;
        this.seasonRepo = seasonRepo;
        this.episodeRepo = episodeRepo;
    }

    @Override
    public void validate(Media current, Media updated) {
        if (current.getType() == updated.getType()) {
            return;
        }

        Long mediaId = current.getId();

        boolean hasWatchEntries = watchRepo.existsByMediaId(mediaId);
        boolean hasSeasons = seasonRepo.existsByMediaId(mediaId);
        boolean hasEpisodes = episodeRepo.existsByMediaId(mediaId);

        if (hasWatchEntries || hasSeasons || hasEpisodes) {
            throw DomainException.of(
                    ErrorCode.MEDIA_TYPE_IMMUTABLE_WITH_HISTORY,
                    mediaId,
                    current.getType().name(),
                    updated.getType().name());
        }
    }
}
