package com.cine.cinelog.features.batch.jobs.seasons;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.model.tmdb.TmdbEpisodeDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSeasonDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writer que persiste temporadas e episódios buscados do TMDB, aplicando
 * idempotência via existsByMediaIdAndSeasonNumber /
 * existsBySeasonIdAndEpisodeNumber.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeasonsEpisodesItemWriter implements ItemWriter<TmdbSeasonsBundle> {

    private final SeasonRepositoryPort seasonRepository;
    private final EpisodeRepositoryPort episodeRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends TmdbSeasonsBundle> chunk) {
        for (TmdbSeasonsBundle bundle : chunk) {
            for (TmdbSeasonDetails seasonDetails : bundle.getSeasons()) {
                persistSeason(bundle.getMediaId(), seasonDetails);
            }
        }
    }

    private void persistSeason(Long mediaId, TmdbSeasonDetails seasonDetails) {
        Integer seasonNumber = seasonDetails.getSeasonNumber();

        // Resolve or create the season
        Season season = seasonRepository.findByMediaIdAndSeasonNumber(mediaId, seasonNumber)
                .orElseGet(() -> {
                    Season newSeason = new Season();
                    newSeason.setMediaId(mediaId);
                    newSeason.setSeasonNumber(seasonNumber);
                    newSeason.setName(seasonDetails.getName());
                    newSeason.setAirDate(seasonDetails.getAirDate());
                    Season saved = seasonRepository.save(newSeason);
                    log.debug("Saved season {}/{}", mediaId, seasonNumber);
                    return saved;
                });

        // Persist episodes
        if (seasonDetails.getEpisodes() != null) {
            for (TmdbEpisodeDetails ep : seasonDetails.getEpisodes()) {
                persistEpisode(season.getId(), ep);
            }
        }
    }

    private void persistEpisode(Long seasonId, TmdbEpisodeDetails epDetails) {
        Integer episodeNumber = epDetails.getEpisodeNumber();
        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(seasonId, episodeNumber)) {
            return;
        }
        Episode episode = new Episode();
        episode.setSeasonId(seasonId);
        episode.setEpisodeNumber(episodeNumber);
        episode.setName(epDetails.getName());
        episode.setAirDate(epDetails.getAirDate());
        episodeRepository.save(episode);
        log.debug("Saved episode s={} ep={}", seasonId, episodeNumber);
    }
}
