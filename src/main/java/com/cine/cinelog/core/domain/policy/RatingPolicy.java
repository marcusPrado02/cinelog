package com.cine.cinelog.core.domain.policy;

import java.time.Instant;

import com.cine.cinelog.core.domain.model.WatchEntry;

public interface RatingPolicy {
    void validateCanRate(WatchEntry entry, Integer rating, Instant when);
}