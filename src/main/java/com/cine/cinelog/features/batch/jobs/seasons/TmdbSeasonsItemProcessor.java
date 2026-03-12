package com.cine.cinelog.features.batch.jobs.seasons;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSeasonDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Processor que, para cada série, busca os detalhes no TMDB e obtém todas as
 * temporadas via fetchTvSeason, retornando um TmdbSeasonsBundle com os dados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbSeasonsItemProcessor implements ItemProcessor<Media, TmdbSeasonsBundle> {

    private final TmdbClientPort tmdbClient;

    @Override
    public TmdbSeasonsBundle process(Media media) {
        Long tmdbId = media.getTmdbId();

        // Fetch TV details to know how many seasons exist
        Optional<TmdbMediaDetails> detailsOpt = tmdbClient.fetchTv(tmdbId);
        if (detailsOpt.isEmpty()) {
            log.warn("TMDB details not found for series mediaId={} tmdbId={}", media.getId(), tmdbId);
            return null;
        }

        TmdbMediaDetails details = detailsOpt.get();
        Integer numberOfSeasons = details.getNumberOfSeasons();
        if (numberOfSeasons == null || numberOfSeasons <= 0) {
            log.debug("No seasons info for tmdbId={}", tmdbId);
            return null;
        }

        List<TmdbSeasonDetails> seasons = new ArrayList<>();
        for (int seasonNumber = 1; seasonNumber <= numberOfSeasons; seasonNumber++) {
            Optional<TmdbSeasonDetails> seasonOpt = tmdbClient.fetchTvSeason(tmdbId, seasonNumber);
            if (seasonOpt.isPresent()) {
                seasons.add(seasonOpt.get());
            } else {
                log.debug("Season {}/{} not found for tmdbId={}", seasonNumber, numberOfSeasons, tmdbId);
            }
        }

        if (seasons.isEmpty()) {
            return null;
        }

        return new TmdbSeasonsBundle(media.getId(), tmdbId, seasons);
    }
}
