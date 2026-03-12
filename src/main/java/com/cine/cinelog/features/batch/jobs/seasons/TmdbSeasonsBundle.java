package com.cine.cinelog.features.batch.jobs.seasons;

import com.cine.cinelog.core.domain.model.tmdb.TmdbSeasonDetails;

import java.util.List;

/**
 * Wrapper que agrega uma lista de temporadas TMDB com o ID local da série.
 */
public class TmdbSeasonsBundle {

    private final Long mediaId;
    private final Long tmdbId;
    private final List<TmdbSeasonDetails> seasons;

    public TmdbSeasonsBundle(Long mediaId, Long tmdbId, List<TmdbSeasonDetails> seasons) {
        this.mediaId = mediaId;
        this.tmdbId = tmdbId;
        this.seasons = seasons;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public List<TmdbSeasonDetails> getSeasons() {
        return seasons;
    }
}
