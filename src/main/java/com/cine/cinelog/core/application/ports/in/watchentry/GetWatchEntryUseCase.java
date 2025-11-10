package com.cine.cinelog.core.application.ports.in.watchentry;

import com.cine.cinelog.core.domain.model.WatchEntry;

/**
 * Caso de uso para obter uma entrada do histórico (WatchEntry) pelo seu ID.
 * Essa interface define o contrato da aplicação sem dependências de
 * infraestrutura.
 */
public interface GetWatchEntryUseCase {
    /**
     * Obtém uma entrada do histórico (WatchEntry) pelo seu ID.
     * 
     * @param id
     * @return
     */
    WatchEntry execute(Long id);
}
