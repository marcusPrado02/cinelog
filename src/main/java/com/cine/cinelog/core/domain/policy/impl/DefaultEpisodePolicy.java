package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.core.domain.policy.EpisodePolicy;
/**
 * Política de domínio para gerenciamento de defaultepisode.
 * Define as regras e validações relacionadas a defaultepisode.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultEpisode.</p>
 * 
 * @since 1.0
 * @see DefaultEpisode
 */

@Component
public class DefaultEpisodePolicy implements EpisodePolicy {

    private static final int MAX_NAME_LENGTH = 200;

    @Override
    public void validateCreate(Episode episode) {
        common(episode);
    }

    @Override
    public void validateUpdate(Episode episode) {
        common(episode);
    }

    private void common(Episode episode) {
        if (episode == null) {
            throw DomainException.of(ErrorCode.EPISODE_INVALID);
        }

        // E1: seasonId obrigatório
        if (episode.getSeasonId() == null) {
            throw DomainException.of(
                    ErrorCode.EPISODE_INVALID,
                    "seasonId is required");
        }

        // E2: episodeNumber obrigatório e >= 1
        if (episode.getEpisodeNumber() == null || episode.getEpisodeNumber() < 1) {
            throw DomainException.of(
                    ErrorCode.EPISODE_NUMBER_INVALID,
                    episode.getEpisodeNumber());
        }

        // E4: name + airDate
        if (episode.getName() != null) {
            String trimmed = episode.getName().trim();
            if (trimmed.isEmpty()) {
                episode.setName(null);
            } else {
                if (trimmed.length() > MAX_NAME_LENGTH) {
                    throw DomainException.of(
                            ErrorCode.EPISODE_INVALID,
                            "name too long",
                            MAX_NAME_LENGTH,
                            trimmed.length());
                }
                episode.setName(trimmed);
            }
        }

        if (episode.getAirDate() != null) {
            LocalDate today = LocalDate.now();
            if (episode.getAirDate().isAfter(today)) {
                throw DomainException.of(
                        ErrorCode.EPISODE_INVALID,
                        "airDate cannot be in the future");
            }
        }
    }
}
