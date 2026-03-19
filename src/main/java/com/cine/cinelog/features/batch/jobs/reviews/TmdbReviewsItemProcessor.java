package com.cine.cinelog.features.batch.jobs.reviews;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewResult;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewsPage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Busca reviews do TMDB para uma mídia e retorna um bundle com todos os
 * resultados coletados (até 3 páginas).
 */
@Slf4j
@Component
public class TmdbReviewsItemProcessor implements ItemProcessor<Media, ReviewsBundle> {

    private static final int MAX_PAGES = 3;

    private final TmdbClientPort tmdbClient;

    public TmdbReviewsItemProcessor(TmdbClientPort tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    @Override
    public ReviewsBundle process(Media media) {
        if (media.getTmdbId() == null) {
            return null;
        }

        List<TmdbReviewResult> allReviews = new ArrayList<>();

        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                TmdbReviewsPage reviewsPage = fetchPage(media, page);
                if (reviewsPage == null || reviewsPage.getResults() == null || reviewsPage.getResults().isEmpty()) {
                    break;
                }
                allReviews.addAll(reviewsPage.getResults());
                if (page >= reviewsPage.getTotalPages()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao buscar reviews para mídia id={} tmdbId={}: {}",
                    media.getId(), media.getTmdbId(), e.getMessage());
        }

        if (allReviews.isEmpty()) {
            return null;
        }

        return new ReviewsBundle(media, allReviews);
    }

    private TmdbReviewsPage fetchPage(Media media, int page) {
        if (media.getType() == MediaType.MOVIE) {
            return tmdbClient.fetchMovieReviews(media.getTmdbId(), page);
        } else {
            return tmdbClient.fetchTvReviews(media.getTmdbId(), page);
        }
    }
}
