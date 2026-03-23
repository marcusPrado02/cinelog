package com.cine.cinelog.features.batch.jobs.images;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

/**
 * Busca detalhes da mídia no TMDB e preenche posterUrl e backdropUrl.
 */
@Slf4j
@Component
public class TmdbMediaImageProcessor implements ItemProcessor<Media, Media> {

    private final TmdbClientPort tmdbClient;

    public TmdbMediaImageProcessor(TmdbClientPort tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    @Override
    public Media process(Media media) {
        if (media.getTmdbId() == null) {
            return null;
        }

        try {
            Optional<TmdbMediaDetails> detailsOpt = media.getType() == MediaType.MOVIE
                    ? tmdbClient.fetchMovie(media.getTmdbId())
                    : tmdbClient.fetchTv(media.getTmdbId());

            if (detailsOpt.isEmpty()) {
                return null;
            }

            TmdbMediaDetails details = detailsOpt.get();
            if (details.getPosterUrl() == null && details.getBackdropUrl() == null) {
                return null;
            }

            if (details.getPosterUrl() != null) {
                media.setPosterUrl(details.getPosterUrl());
            }
            if (details.getBackdropUrl() != null) {
                media.setBackdropUrl(details.getBackdropUrl());
            }

            return media;

        } catch (WebClientResponseException | RestClientException e) {
            // Erros de rede/API — propagam para o retry do Spring Batch
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de persistencia ao enriquecer imagens para midia id={}: {}",
                    media.getId(), e.getMessage());
            return null;
        } catch (RuntimeException e) {
            log.warn("Falha ao buscar imagens para midia id={} tmdbId={}: {}",
                    media.getId(), media.getTmdbId(), e.getMessage());
            return null;
        }
    }
}
