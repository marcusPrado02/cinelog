package com.cine.cinelog.core.application.ports.in.watchentry;

/**
 * Caso de uso para deletar uma watch entry.
 */
public interface DeleteWatchEntryUseCase {
    /**
     * Deleta o registro de watch entry pelo ID.
     * 
     * @param id
     */
    void execute(Long id);
}
