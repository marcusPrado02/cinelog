package com.cine.cinelog.features.batch.jobs.credits;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * Leitor que retorna todas as mídias com tmdbId definido para o job de
 * créditos.
 * Lê filmes e séries em sequência.
 */
@Component
public class MediaWithTmdbIdItemReader implements ItemReader<Media> {

    private final MediaRepositoryPort mediaRepository;

    private Iterator<Media> iterator;

    public MediaWithTmdbIdItemReader(MediaRepositoryPort mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Media read() {
        if (iterator == null) {
            List<Media> movies = mediaRepository.findAllWithTmdbId(MediaType.MOVIE);
            List<Media> series = mediaRepository.findAllWithTmdbId(MediaType.SERIES);
            java.util.List<Media> all = new java.util.ArrayList<>(movies);
            all.addAll(series);
            iterator = all.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
