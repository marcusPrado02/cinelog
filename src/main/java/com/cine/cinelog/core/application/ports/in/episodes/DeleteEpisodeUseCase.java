package com.cine.cinelog.core.application.ports.in.episodes;

/**
 * Caso de uso para exclusão de episódios.
 * Define a operação de remoção de um episódio existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface DeleteEpisodeUseCase {
    void execute(Long id);
}
