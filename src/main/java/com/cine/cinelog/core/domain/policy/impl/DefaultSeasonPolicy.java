package com.cine.cinelog.core.domain.policy.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.policy.SeasonPolicy;

/**
 * Política de domínio para gerenciamento de defaultseason.
 * Define as regras e validações relacionadas a defaultseason.
 *
 * <p>
 * Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultSeason.
 * </p>
 *
 * @since 1.0
 * @see DefaultSeason
 */
@Component
public class DefaultSeasonPolicy implements SeasonPolicy {

    private static final int MAX_NAME_LENGTH = 200;

    @Override
    public void validateCreate(Season season) {
        common(season);
    }

    @Override
    public void validateUpdate(Season season) {
        common(season);
    }

    private void common(Season season) {
        if (season == null) {
            throw DomainException.of(ErrorCode.SEASON_INVALID);
        }

        // S1: mediaId obrigatório
        if (season.getMediaId() == null) {
            throw DomainException.of(
                    ErrorCode.SEASON_INVALID,
                    "mediaId is required");
        }

        // S2: seasonNumber obrigatório e >= 1
        if (season.getSeasonNumber() == null || season.getSeasonNumber() < 1) {
            throw DomainException.of(
                    ErrorCode.SEASON_NUMBER_INVALID,
                    season.getSeasonNumber());
        }

        // S4: name opcional, trim + limite
        if (season.getName() != null) {
            String trimmed = season.getName().trim();
            if (trimmed.isEmpty()) {
                season.setName(null);
            } else {
                if (trimmed.length() > MAX_NAME_LENGTH) {
                    throw DomainException.of(
                            ErrorCode.SEASON_INVALID,
                            "name too long",
                            MAX_NAME_LENGTH,
                            trimmed.length());
                }
                season.setName(trimmed);
            }
        }

        // (extra sênior opcional) airDate, se informada, não futura
        if (season.getAirDate() != null) {
            LocalDate today = LocalDate.now();
            if (season.getAirDate().isAfter(today)) {
                throw DomainException.of(
                        ErrorCode.SEASON_INVALID,
                        "airDate cannot be in the future");
            }
        }
    }
}
