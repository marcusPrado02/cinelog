package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.WatchlistItem;

/**
 * Regras exclusivas de atualização de Watchlist:
 *
 * WL6: userId não pode ser alterado.
 * WL7: mediaId não pode ser alterado.
 */
public interface WatchlistUpdatePolicy {

    void validate(WatchlistItem current, WatchlistItem updated);
}
