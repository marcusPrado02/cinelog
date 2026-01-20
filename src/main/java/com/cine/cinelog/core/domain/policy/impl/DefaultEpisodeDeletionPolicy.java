package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.core.domain.policy.EpisodeDeletionPolicy;
/**
 * Política de domínio para gerenciamento de defaultepisodedeletion.
 * Define as regras e validações relacionadas a defaultepisodedeletion.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultEpisodeDeletion.</p>
 * 
 * @since 1.0
 * @see DefaultEpisodeDeletion
 */

@Component
public class DefaultEpisodeDeletionPolicy implements EpisodeDeletionPolicy {

    private final WatchEntryRepositoryPort watchEntryRepo;

    public DefaultEpisodeDeletionPolicy(WatchEntryRepositoryPort watchEntryRepo) {
        this.watchEntryRepo = watchEntryRepo;
    }

    @Override
    public void validateDelete(Episode episode) {
        if (episode == null || episode.getId() == null) {
            throw DomainException.of(ErrorCode.EPISODE_DELETE_FORBIDDEN);
        }

        Long episodeId = episode.getId();

        boolean hasWatchEntries = watchEntryRepo.existsByEpisodeId(episodeId);

        if (!hasWatchEntries) {
            return; // pode excluir
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("episodeId", episodeId);
        details.put("hasWatchEntries", hasWatchEntries);

        throw DomainException.of(ErrorCode.EPISODE_DELETE_FORBIDDEN, details);
    }
}
