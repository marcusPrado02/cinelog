package com.cine.cinelog.features.batch.jobs.seasons;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * ItemReader que fornece todas as séries de TV que possuem tmdbId para o job
 * de importação de temporadas e episódios.
 */
@Component
public class TvSeriesItemReader implements ItemReader<Media> {

    private final MediaRepositoryPort mediaRepository;

    private Iterator<Media> iterator;

    public TvSeriesItemReader(MediaRepositoryPort mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Media read() {
        if (iterator == null) {
            List<Media> series = mediaRepository.findAllWithTmdbId(MediaType.SERIES);
            iterator = series.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
