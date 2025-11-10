package com.cine.cinelog.core.application.ports.in.watchentry;

import com.cine.cinelog.core.domain.model.WatchEntry;

/**
 * Caso de uso para atualizar uma entrada do histórico (WatchEntry).
 * Essa interface define o contrato da aplicação sem dependências de
 * infraestrutura.
 */
public interface UpdateWatchEntryUseCase {

    /**
     * Atualiza uma entrada do histórico (WatchEntry).
     * 
     * @param entry
     * @param isRatingOperation
     * @return
     */
    WatchEntry execute(WatchEntry entry, boolean isRatingOperation);

}
