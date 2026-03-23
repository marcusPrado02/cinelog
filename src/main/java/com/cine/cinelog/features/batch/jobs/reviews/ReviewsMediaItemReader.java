package com.cine.cinelog.features.batch.jobs.reviews;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Leitor que retorna todas as mídias com tmdbId definido para o job de
 * sincronização de reviews.
 */
@Component
@StepScope
public class ReviewsMediaItemReader implements ItemReader<Media> {

    private final MediaRepositoryPort mediaRepository;

    private Iterator<Media> iterator;

    public ReviewsMediaItemReader(MediaRepositoryPort mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Media read() {
        if (iterator == null) {
            List<Media> movies = mediaRepository.findAllWithTmdbId(MediaType.MOVIE);
            List<Media> series = mediaRepository.findAllWithTmdbId(MediaType.SERIES);
            List<Media> all = new ArrayList<>(movies);
            all.addAll(series);
            iterator = all.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
