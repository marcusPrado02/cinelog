package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.WatchEntry;

public interface WatchEntryPolicy {
    void validateCreate(WatchEntry entry);

    void validateUpdate(WatchEntry entry);
}