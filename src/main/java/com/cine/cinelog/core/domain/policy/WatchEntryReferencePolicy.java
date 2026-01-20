package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.WatchEntry;

/**
 * Regras relacionadas a referências de mídia/episódio.
 * W6, W7, W8, W10
 */
public interface WatchEntryReferencePolicy {

    void validateCreate(WatchEntry entry);

    void validateUpdate(WatchEntry current, WatchEntry updated);
}