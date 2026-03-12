package com.cine.cinelog.features.batch.jobs.media;

import com.cine.cinelog.core.domain.model.Media;

import java.util.List;

/**
 * Wrapper para transportar uma {@link Media} e a lista de nomes de gêneros
 * associados do TMDB entre o processor e o writer do batch de importação.
 */
public class MediaWithGenres {

    private final Media media;
    private final List<String> genreNames;

    public MediaWithGenres(Media media, List<String> genreNames) {
        this.media = media;
        this.genreNames = genreNames;
    }

    public Media getMedia() {
        return media;
    }

    public List<String> getGenreNames() {
        return genreNames;
    }
}
