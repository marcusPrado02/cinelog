package com.cine.cinelog.core.application.ports.in.season;

/**
 * Caso de uso para exclusão de temporadas.
 * Define a operação de remoção de uma temporada existente no sistema,
 * identificando-a pelo seu ID.
 */
public interface DeleteSeasonUseCase {
    /**
     * Remove uma temporada existente do sistema.
     *
     * @param id O ID da temporada a ser removida.
     */
    void execute(Long id);
}
