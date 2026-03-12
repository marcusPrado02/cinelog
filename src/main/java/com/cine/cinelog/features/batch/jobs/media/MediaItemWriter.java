package com.cine.cinelog.features.batch.jobs.media;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.application.ports.out.MediaGenreLinkPort;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.Media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Writer que persiste {@link MediaWithGenres} no banco de dados.
 *
 * <p>
 * Para cada item: salva ou atualiza a mídia e atualiza os vínculos de gênero.
 * </p>
 */
@Component
public class MediaItemWriter implements ItemWriter<MediaWithGenres> {

    private static final Logger log = LoggerFactory.getLogger(MediaItemWriter.class);

    private final MediaRepositoryPort mediaRepository;
    private final GenreRepositoryPort genreRepository;
    private final MediaGenreLinkPort mediaGenreLinkPort;

    public MediaItemWriter(MediaRepositoryPort mediaRepository,
            GenreRepositoryPort genreRepository,
            MediaGenreLinkPort mediaGenreLinkPort) {
        this.mediaRepository = mediaRepository;
        this.genreRepository = genreRepository;
        this.mediaGenreLinkPort = mediaGenreLinkPort;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends MediaWithGenres> chunk) {
        for (MediaWithGenres item : chunk) {
            try {
                Media saved = mediaRepository.save(item.getMedia());

                List<Long> genreIds = new ArrayList<>();
                if (item.getGenreNames() != null) {
                    for (String name : item.getGenreNames()) {
                        genreRepository.findByName(name).ifPresent(g -> genreIds.add(g.getId()));
                    }
                }

                if (saved.getId() != null && !genreIds.isEmpty()) {
                    mediaGenreLinkPort.replaceGenres(saved.getId(), genreIds);
                }

            } catch (Exception e) {
                log.error("Erro ao salvar mídia tmdbId={}: {}", item.getMedia().getTmdbId(), e.getMessage());
            }
        }
    }
}
