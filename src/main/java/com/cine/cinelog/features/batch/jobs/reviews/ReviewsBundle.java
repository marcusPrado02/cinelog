package com.cine.cinelog.features.batch.jobs.reviews;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewResult;

import java.util.List;

/**
 * Agrega uma mídia e suas reviews do TMDB para processamento em lote.
 */
public record ReviewsBundle(Media media, List<TmdbReviewResult> reviews) {
}
