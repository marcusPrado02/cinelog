package com.cine.cinelog.features.batch.jobs.images;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persiste as mídias com imagens atualizadas.
 */
@Slf4j
@Component
public class MediaImageUpdateWriter implements ItemWriter<Media> {

    private final MediaRepositoryPort mediaRepository;

    public MediaImageUpdateWriter(MediaRepositoryPort mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Override
    public void write(Chunk<? extends Media> chunk) {
        for (Media media : chunk) {
            try {
                mediaRepository.save(media);
                log.debug("Imagens atualizadas para mídia id={}", media.getId());
            } catch (Exception e) {
                log.warn("Erro ao salvar imagens para mídia id={}: {}", media.getId(), e.getMessage());
            }
        }
    }
}
