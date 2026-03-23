package com.cine.cinelog.features.batch.jobs.images;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * Leitor que retorna mídias com tmdbId definido mas sem imagens (poster ou
 * backdrop) para enriquecimento via TMDB.
 */
@Component
@StepScope
public class MediaMissingImagesItemReader implements ItemReader<Media> {

    private final MediaRepositoryPort mediaRepository;

    private Iterator<Media> iterator;

    public MediaMissingImagesItemReader(MediaRepositoryPort mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Media read() {
        if (iterator == null) {
            List<Media> missing = mediaRepository.findAllMissingImages();
            iterator = missing.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
