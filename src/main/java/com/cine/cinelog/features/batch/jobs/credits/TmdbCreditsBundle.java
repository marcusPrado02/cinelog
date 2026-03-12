package com.cine.cinelog.features.batch.jobs.credits;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;

/**
 * Wrapper que agrega créditos TMDB com o ID local da mídia.
 */
public class TmdbCreditsBundle {

    private final Long mediaId;
    private final Long tmdbId;
    private final MediaType mediaType;
    private final TmdbCredits credits;

    public TmdbCreditsBundle(Long mediaId, Long tmdbId, MediaType mediaType, TmdbCredits credits) {
        this.mediaId = mediaId;
        this.tmdbId = tmdbId;
        this.mediaType = mediaType;
        this.credits = credits;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public TmdbCredits getCredits() {
        return credits;
    }
}
