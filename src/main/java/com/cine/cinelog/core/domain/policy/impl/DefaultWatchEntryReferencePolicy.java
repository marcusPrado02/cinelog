package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.WatchEntryReferencePolicy;

/**
 * Regras de referência da WatchEntry:
 *
 * W6: Se episodeId != null → episódio deve existir
 * W7: mediaId deve existir
 * W8: episódio deve pertencer à season, e esta pertencer à mesma mediaId
 * W9: userId é imutável
 * W10: se houver episodeId, a mídia deve ser do tipo SERIES
 */
@Component
public class DefaultWatchEntryReferencePolicy implements WatchEntryReferencePolicy {

    private final MediaRepositoryPort mediaRepo;
    private final SeasonRepositoryPort seasonRepo;
    private final EpisodeRepositoryPort episodeRepo;

    public DefaultWatchEntryReferencePolicy(MediaRepositoryPort mediaRepo,
            SeasonRepositoryPort seasonRepo,
            EpisodeRepositoryPort episodeRepo) {
        this.mediaRepo = mediaRepo;
        this.seasonRepo = seasonRepo;
        this.episodeRepo = episodeRepo;
    }

    @Override
    public void validateCreate(WatchEntry entry) {
        validateReferences(entry);
    }

    @Override
    public void validateUpdate(WatchEntry current, WatchEntry updated) {
        validateReferences(updated);
    }

    private void validateReferences(WatchEntry entry) {

        // ===== W7: mediaId deve existir (se informado)
        if (entry.getMediaId() != null && !mediaRepo.existsById(entry.getMediaId())) {
            throw DomainException.of(
                    ErrorCode.MEDIA_NOT_FOUND,
                    entry.getMediaId());
        }

        // ===== W6: episodeId informado → episódio deve existir
        if (entry.getEpisodeId() != null &&
                !episodeRepo.existsById(entry.getEpisodeId())) {

            throw DomainException.of(
                    ErrorCode.INVALID_WATCH_ENTRY,
                    "Referenced episode not found: " + entry.getEpisodeId());
        }

        // Se não houver episodeId → não existe W8/W10 a validar
        if (entry.getEpisodeId() == null) {
            return;
        }

        // ===== W8: episódio deve pertencer a uma season,
        // e esta deve pertencer à mesma mídia
        var episode = episodeRepo.findById(entry.getEpisodeId())
                .orElseThrow(() -> DomainException.of(
                        ErrorCode.INVALID_WATCH_ENTRY,
                        "Episode not found: " + entry.getEpisodeId()));

        var season = seasonRepo.findById(episode.getSeasonId())
                .orElseThrow(() -> DomainException.of(
                        ErrorCode.INVALID_WATCH_ENTRY,
                        "Season not found for episode: " + episode.getId()));

        Long episodeMedia = season.getMediaId();
        Long providedMedia = entry.getMediaId();

        if (providedMedia == null || !providedMedia.equals(episodeMedia)) {
            throw DomainException.of(
                    ErrorCode.INVALID_WATCH_ENTRY,
                    "episode does not belong to the referenced media");
        }

        // ===== W10: episódios só podem existir para SERIES
        var media = mediaRepo.findById(providedMedia)
                .orElseThrow(() -> DomainException.of(
                        ErrorCode.MEDIA_NOT_FOUND, providedMedia));

        if (!media.isSeries()) {
            throw DomainException.of(
                    ErrorCode.INVALID_WATCH_ENTRY,
                    "episodes can only be attached to SERIES media type");
        }
    }
}
