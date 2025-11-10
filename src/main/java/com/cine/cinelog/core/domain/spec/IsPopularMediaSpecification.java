package com.cine.cinelog.core.domain.spec;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.vo.Rating;

// Exemplo: "popular" se média >= 7 e lançado nos últimos 10 anos 
public final class IsPopularMediaSpecification {

    public boolean isSatisfiedBy(Media media, double averageRating) {
        int year = media.getReleaseYear();
        int current = java.time.Year.now().getValue();
        boolean recent = (current - year) <= 10;
        return recent && averageRating >= 7.0;
    }

    // Overload simples para VO Rating
    public boolean isSatisfiedBy(Media media, Rating avg) {
        return isSatisfiedBy(media, avg.value());
    }
}