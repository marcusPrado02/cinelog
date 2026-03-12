package com.cine.cinelog.features.batch.jobs.credits;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Processor que busca os créditos de uma mídia no TMDB e encapsula
 * no wrapper {@link TmdbCreditsBundle}.
 */
@Component
public class TmdbCreditsItemProcessor implements ItemProcessor<Media, TmdbCreditsBundle> {

    private static final Logger log = LoggerFactory.getLogger(TmdbCreditsItemProcessor.class);

    private final TmdbClientPort tmdbClient;

    public TmdbCreditsItemProcessor(TmdbClientPort tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    @Override
    public TmdbCreditsBundle process(@NonNull Media media) {
        if (media.getTmdbId() == null) {
            return null;
        }

        Optional<TmdbCredits> creditsOpt;
        if (media.getType() == MediaType.MOVIE) {
            creditsOpt = tmdbClient.fetchMovieCredits(media.getTmdbId());
        } else {
            creditsOpt = tmdbClient.fetchTvCredits(media.getTmdbId());
        }

        if (creditsOpt.isEmpty()) {
            log.debug("Créditos não encontrados para mediaId={} tmdbId={}", media.getId(), media.getTmdbId());
            return null;
        }

        return new TmdbCreditsBundle(media.getId(), media.getTmdbId(), media.getType(), creditsOpt.get());
    }
}
