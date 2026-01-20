package com.cine.cinelog.core.domain.policy.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaDeletionPolicy;

/**
 * Implementação padrão da MediaDeletionPolicy.
 *
 * Aplica as regras MD1–MD5 usando os repositórios de agregados relacionados.
 */
@Component
public class DefaultMediaDeletionPolicy implements MediaDeletionPolicy {

    private final SeasonRepositoryPort seasonRepo;
    private final EpisodeRepositoryPort episodeRepo;
    private final WatchEntryRepositoryPort watchEntryRepo;
    private final WatchlistRepositoryPort watchlistRepo;

    public DefaultMediaDeletionPolicy(SeasonRepositoryPort seasonRepo,
            EpisodeRepositoryPort episodeRepo,
            WatchEntryRepositoryPort watchEntryRepo,
            WatchlistRepositoryPort watchlistRepo) {
        this.seasonRepo = seasonRepo;
        this.episodeRepo = episodeRepo;
        this.watchEntryRepo = watchEntryRepo;
        this.watchlistRepo = watchlistRepo;
    }

    @Override
    public void validateDelete(Media media) {
        if (media == null || media.getId() == null) {
            throw DomainException.of(ErrorCode.MEDIA_DELETE_FORBIDDEN);
        }

        Long mediaId = media.getId();

        // MD1: possui seasons associadas?
        boolean hasSeasons = seasonRepo.existsByMediaId(mediaId);

        // MD2: possui episodes associados?
        boolean hasEpisodes = episodeRepo.existsByMediaId(mediaId);

        // MD3: possui histórico de watch entries?
        boolean hasWatchEntries = watchEntryRepo.existsByMediaId(mediaId);

        // MD4: aparece em alguma watchlist?
        boolean inWatchlist = watchlistRepo.existsByMediaId(mediaId);

        // MD5: possui vínculo externo (tmdbId)?
        boolean hasExternalProvider = media.hasTmdbId();

        if (!hasSeasons && !hasEpisodes && !hasWatchEntries && !inWatchlist && !hasExternalProvider) {
            // sem histórico nem vínculos → deleção permitida
            return;
        }

        // Monta um mapa de detalhes pra ficar claro o motivo do bloqueio
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mediaId", mediaId);
        details.put("hasSeasons", hasSeasons);
        details.put("hasEpisodes", hasEpisodes);
        details.put("hasWatchEntries", hasWatchEntries);
        details.put("inWatchlist", inWatchlist);
        details.put("hasExternalProvider", hasExternalProvider);

        throw DomainException.of(ErrorCode.MEDIA_DELETE_FORBIDDEN, details);
    }
}
