package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.WatchlistItem;

/**
 * Regras de referência cruzada com Media:
 *
 * WL8: mídia deve existir.
 * WL9: tipo de mídia deve ser permitido na watchlist.
 * WL10: ano de lançamento não pode ser muito distante no futuro.
 */
public interface WatchlistReferencePolicy {

    void validateCreate(WatchlistItem item);

}
