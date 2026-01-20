package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.core.domain.policy.EpisodeUniquenessPolicy;
/**
 * Política de domínio para gerenciamento de defaultepisodeuniqueness.
 * Define as regras e validações relacionadas a defaultepisodeuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultEpisodeUniqueness.</p>
 * 
 * @since 1.0
 * @see DefaultEpisodeUniqueness
 */

@Component
public class DefaultEpisodeUniquenessPolicy implements EpisodeUniquenessPolicy {

    private final EpisodeRepositoryPort repo;

    public DefaultEpisodeUniquenessPolicy(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void validateCreate(Episode episode) {
        if (episode.getSeasonId() == null || episode.getEpisodeNumber() == null) {
            return;
        }

        boolean exists = repo.existsBySeasonIdAndEpisodeNumber(
                episode.getSeasonId(),
                episode.getEpisodeNumber());

        if (exists) {
            throw DomainException.of(
                    ErrorCode.EPISODE_DUPLICATE,
                    episode.getSeasonId(),
                    episode.getEpisodeNumber());
        }
    }
}
