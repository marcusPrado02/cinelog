package com.cine.cinelog.core.domain.policy.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.RatingPolicy;

public class DefaultRatingPolicy implements RatingPolicy {

    private final int min; // ex.: 0
    private final int max; // ex.: 10
    private final int maxDaysSkew; // ex.: 2 (quanto tempo antes/depois do watchedAt aceitamos)

    public DefaultRatingPolicy(int min, int max, int maxDaysSkew) {
        this.min = min;
        this.max = max;
        this.maxDaysSkew = maxDaysSkew;
    }

    @Override
    public void validateCanRate(WatchEntry entry, Integer rating, Instant when) {
        if (rating == null) {
            throw DomainException.of(ErrorCode.INVALID_ARGUMENT, "rating must not be null");
        }
        if (rating < min || rating > max) {
            throw DomainException.of(ErrorCode.RATING_NOT_ALLOWED,
                    "rating out of bounds",
                    java.util.Map.of("min", min, "max", max, "value", rating));
        }
        if (entry.getWatchedAt() == null) {
            throw DomainException.of(ErrorCode.RATING_NOT_ALLOWED,
                    "cannot rate before marking as watched");
        }
        if (when == null) {
            when = Instant.now();
        }

        // tolerância: avaliação pode ser até X dias antes/depois do watchedAt
        long days = Math.abs(ChronoUnit.DAYS.between(
                entry.getWatchedAt(),
                when.atOffset(ZoneOffset.UTC).toLocalDate()));

        if (days > maxDaysSkew) {
            throw DomainException.of(ErrorCode.RATING_NOT_ALLOWED,
                    "rating time too far from watchedAt",
                    java.util.Map.of("allowedDays", maxDaysSkew, "offsetDays", days));
        }
    }
}