package com.cine.cinelog.core.domain.policy.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;

public class DefaultWatchEntryPolicy implements WatchEntryPolicy {

    private final int maxCommentLength;
    private final boolean forbidFutureWatch;

    public DefaultWatchEntryPolicy(int maxCommentLength, boolean forbidFutureWatch) {
        this.maxCommentLength = maxCommentLength;
        this.forbidFutureWatch = forbidFutureWatch;
    }

    @Override
    public void validateCreate(WatchEntry entry) {
        common(entry);
    }

    @Override
    public void validateUpdate(WatchEntry entry) {
        common(entry);
    }

    private void common(WatchEntry e) {
        if (e == null) {
            throw DomainException.of(ErrorCode.INVALID_ARGUMENT, "watchEntry must not be null");
        }
        if (e.getComment() != null && e.getComment().length() > maxCommentLength) {
            throw DomainException.of(
                    ErrorCode.INVALID_WATCH_ENTRY,
                    "comment too long",
                    Map.of("max", maxCommentLength, "length", e.getComment().length()));
        }
        if (forbidFutureWatch && e.getWatchedAt() != null) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (e.getWatchedAt().isAfter(now.toLocalDate())) {
                throw DomainException.of(
                        ErrorCode.INVALID_WATCH_ENTRY,
                        "watchedAt cannot be in the future",
                        Map.of("watchedAt", e.getWatchedAt().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond(),
                                "now", now.toInstant().getEpochSecond()));
            }
        }
    }
}
