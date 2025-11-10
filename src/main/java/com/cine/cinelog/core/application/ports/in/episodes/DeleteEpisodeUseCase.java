package com.cine.cinelog.core.application.ports.in.episodes;

/**
 * Caso de uso para exclusão de episódios.
 * Define a operação de remoção de um episódio existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface DeleteEpisodeUseCase {
    /**
     * Remove um episódio existente do sistema.
     *
     * @param id O ID do episódio a ser removido.
     */
    void execute(Long id);
}
