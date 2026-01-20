package com.cine.cinelog.core.domain.policy.impl;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.vo.Rating;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

/**
 * Regras de negócio para WatchEntry:
 *
 * R1: Rating deve estar no intervalo permitido (0..10)
 * R2: Comentário com tamanho máximo (ex.: 2000 caracteres)
 * R3: watchedAt não pode ser futura
 * R4: Não é permitido informar rating sem data de assistido
 */
public class DefaultWatchEntryPolicy implements WatchEntryPolicy {

    private static final int MAX_COMMENT_LENGTH = 2000;

    public DefaultWatchEntryPolicy() {
    }

    @Measured("cinelog.policy.watchentry.validate_create")
    @AlertIfSlow(thresholdMs = 100)
    @Override
    public void validateCreate(WatchEntry entry) {
        common(entry);
    }

    @Measured("cinelog.policy.watchentry.validate_update")
    @AlertIfSlow(thresholdMs = 100)
    @Override
    public void validateUpdate(WatchEntry entry) {
        common(entry);
        immutableUserId(entry, entry);
    }

    private void common(WatchEntry entry) {
        if (entry == null) {
            throw DomainException.of(ErrorCode.INVALID_WATCH_ENTRY);
        }

        // R1: rating dentro do intervalo permitido
        if (entry.getRating() != null) {
            Rating rating = Rating.of(entry.getRating());
            entry.setRating(rating.value());
        }

        // R2: comentário com limite de tamanho
        String comment = entry.getComment();
        if (comment != null) {
            String trimmed = comment.trim();
            if (trimmed.length() > MAX_COMMENT_LENGTH) {
                throw DomainException.of(
                        ErrorCode.WATCH_ENTRY_COMMENT_TOO_LONG,
                        MAX_COMMENT_LENGTH,
                        trimmed.length());
            }
            entry.setComment(trimmed.isEmpty() ? null : trimmed);
        }

        // R3: data de assistido não pode ser futura
        LocalDate watchedAt = entry.getWatchedAt();
        if (watchedAt != null) {
            LocalDate today = LocalDate.now();
            if (watchedAt.isAfter(today)) {
                throw DomainException.of(ErrorCode.WATCH_ENTRY_DATE_IN_FUTURE, watchedAt);
            }
        }

        // R4: rating sem data de assistido não é permitido
        if (entry.getRating() != null && entry.getWatchedAt() == null) {
            throw DomainException.of(ErrorCode.WATCH_ENTRY_RATING_WITHOUT_DATE);
        }
    }

    private void immutableUserId(WatchEntry current, WatchEntry updated) {

        /**
         * W9: userId é imutável.
         */
        if (updated.getUserId() != null &&
                !updated.getUserId().equals(current.getUserId())) {

            throw DomainException.of(
                    ErrorCode.INVALID_WATCH_ENTRY,
                    "userId cannot be changed");
        }
    }
}
