package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.policy.SeasonDeletionPolicy;
/**
 * Política de domínio para gerenciamento de defaultseasondeletion.
 * Define as regras e validações relacionadas a defaultseasondeletion.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultSeasonDeletion.</p>
 * 
 * @since 1.0
 * @see DefaultSeasonDeletion
 */

@Component
public class DefaultSeasonDeletionPolicy implements SeasonDeletionPolicy {

    private final EpisodeRepositoryPort episodeRepo;
    private final WatchEntryRepositoryPort watchEntryRepo;

    public DefaultSeasonDeletionPolicy(EpisodeRepositoryPort episodeRepo,
            WatchEntryRepositoryPort watchEntryRepo) {
        this.episodeRepo = episodeRepo;
        this.watchEntryRepo = watchEntryRepo;
    }

    @Override
    public void validateDelete(Season season) {
        if (season == null || season.getId() == null) {
            throw DomainException.of(ErrorCode.SEASON_DELETE_FORBIDDEN);
        }

        Long seasonId = season.getId();

        boolean hasEpisodes = episodeRepo.existsBySeasonId(seasonId);
        // episódios com watch entries vão ser bloqueados na policy de deleção de
        // Episode,
        // mas aqui podemos dar uma visão mais ampla se quiser.

        if (!hasEpisodes) {
            return; // pode deletar
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("seasonId", seasonId);
        details.put("hasEpisodes", hasEpisodes);

        throw DomainException.of(ErrorCode.SEASON_DELETE_FORBIDDEN, details);
    }
}
