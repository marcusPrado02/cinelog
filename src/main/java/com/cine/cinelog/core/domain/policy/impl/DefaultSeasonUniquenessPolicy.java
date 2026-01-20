package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.policy.SeasonUniquenessPolicy;
/**
 * Política de domínio para gerenciamento de defaultseasonuniqueness.
 * Define as regras e validações relacionadas a defaultseasonuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultSeasonUniqueness.</p>
 * 
 * @since 1.0
 * @see DefaultSeasonUniqueness
 */

@Component
public class DefaultSeasonUniquenessPolicy implements SeasonUniquenessPolicy {

    private final SeasonRepositoryPort repo;

    public DefaultSeasonUniquenessPolicy(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void validateCreate(Season season) {
        if (season.getMediaId() == null || season.getSeasonNumber() == null) {
            return;
        }

        boolean exists = repo.existsByMediaIdAndSeasonNumber(
                season.getMediaId(),
                season.getSeasonNumber());

        if (exists) {
            throw DomainException.of(
                    ErrorCode.SEASON_DUPLICATE,
                    season.getMediaId(),
                    season.getSeasonNumber());
        }
    }
}
