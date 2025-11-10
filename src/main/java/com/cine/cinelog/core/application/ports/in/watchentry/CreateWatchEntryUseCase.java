package com.cine.cinelog.core.application.ports.in.watchentry;

import com.cine.cinelog.core.domain.model.WatchEntry;

/**
 * Caso de uso para criar uma nova entrada do histórico (WatchEntry).
 * Essa interface define o contrato da aplicação sem dependências de
 * infraestrutura.
 */
public interface CreateWatchEntryUseCase {

    /**
     * Cria uma nova entrada de histórico (WatchEntry).
     *
     * @param entry a entrada a ser criada
     * @return a entrada criada
     */
    WatchEntry execute(WatchEntry entry);
}
