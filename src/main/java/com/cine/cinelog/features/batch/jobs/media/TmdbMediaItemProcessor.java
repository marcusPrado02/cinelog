package com.cine.cinelog.features.batch.jobs.media;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Processor que converte um {@link TmdbMediaSummary} em
 * {@link MediaWithGenres}.
 *
 * <p>
 * Estratégia idempotente: se a mídia já existe no banco pelo tmdbId,
 * atualiza seus dados; caso contrário, cria uma nova instância.
 * </p>
 */
@Component
@StepScope
public class TmdbMediaItemProcessor implements ItemProcessor<TmdbMediaSummary, MediaWithGenres> {

    private static final Logger log = LoggerFactory.getLogger(TmdbMediaItemProcessor.class);

    private final TmdbClientPort tmdbClient;
    private final MediaRepositoryPort mediaRepository;

    @Value("#{jobParameters['mediaType']}")
    private String mediaTypeParam;

    public TmdbMediaItemProcessor(TmdbClientPort tmdbClient, MediaRepositoryPort mediaRepository) {
        this.tmdbClient = tmdbClient;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public MediaWithGenres process(@NonNull TmdbMediaSummary summary) {
        if (summary.getTmdbId() == null) {
            return null;
        }

        // Busca detalhes completos
        MediaType type = MediaType.valueOf(mediaTypeParam);
        Optional<TmdbMediaDetails> detailsOpt;
        if (type == MediaType.MOVIE) {
            detailsOpt = tmdbClient.fetchMovie(summary.getTmdbId());
        } else {
            detailsOpt = tmdbClient.fetchTv(summary.getTmdbId());
        }

        if (detailsOpt.isEmpty()) {
            log.warn("Detalhes não encontrados para tmdbId={}, pulando.", summary.getTmdbId());
            return null;
        }

        TmdbMediaDetails details = detailsOpt.get();

        // Verifica se já existe no banco
        Optional<Media> existing = mediaRepository.findByTmdbId(summary.getTmdbId());

        Media media;
        if (existing.isPresent()) {
            media = existing.get();
            // Atualiza campos enriquecidos
            media.setTitle(details.getTitle());
            media.setOriginalTitle(details.getOriginalTitle());
            media.setOriginalLanguage(details.getOriginalLanguage());
            media.setOverview(details.getOverview());
            media.setPosterUrl(details.getPosterUrl());
            media.setBackdropUrl(details.getBackdropUrl());
            if (details.getReleaseYear() != null) {
                media.setReleaseYear(details.getReleaseYear());
            }
        } else {
            media = new Media();
            media.setTitle(details.getTitle() != null ? details.getTitle() : "Desconhecido");
            media.setType(type);
            media.setTmdbId(summary.getTmdbId());
            media.setOriginalTitle(details.getOriginalTitle());
            media.setOriginalLanguage(details.getOriginalLanguage());
            media.setOverview(details.getOverview());
            media.setPosterUrl(details.getPosterUrl());
            media.setBackdropUrl(details.getBackdropUrl());
            media.setReleaseYear(details.getReleaseYear());
        }

        return new MediaWithGenres(media, details.getGenres());
    }
}
