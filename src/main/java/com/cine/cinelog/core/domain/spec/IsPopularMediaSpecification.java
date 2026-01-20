package com.cine.cinelog.core.domain.spec;

import java.math.BigDecimal;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.MediaWithRating;
import com.cine.cinelog.core.domain.vo.Rating;

/**
 * ST3: Recomendação só considera mídias com rating mínimo.
 *
 * Exemplo: média >= minAverageRating.
 */
public class IsPopularMediaSpecification {

    private BigDecimal minAverageRating;

    public IsPopularMediaSpecification(BigDecimal minAverageRating) {
        this.minAverageRating = minAverageRating;
    }

    public boolean isSatisfiedBy(Media media, BigDecimal averageRating) {
        int year = media.getReleaseYear();
        int current = java.time.Year.now().getValue();
        boolean recent = (current - year) <= 10;
        return recent && averageRating.compareTo(minAverageRating) >= 0;
    }

    public boolean isSatisfiedBy(MediaWithRating media) {
        if (media == null) {
            return false;
        }
        BigDecimal avg = media.getAverageRating();
        if (avg == null) {
            return false;
        }
        return avg.compareTo(minAverageRating) >= 0;
    }

    // Overload simples para VO Rating
    public boolean isSatisfiedBy(Media media, Rating avg) {
        return isSatisfiedBy(media, avg.value());
    }
}