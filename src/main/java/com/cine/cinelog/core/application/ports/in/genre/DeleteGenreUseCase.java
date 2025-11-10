package com.cine.cinelog.core.application.ports.in.genre;

/**
 * Caso de uso para exclusão de gêneros.
 * Define a operação de remoção de um gênero existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface DeleteGenreUseCase {
    /**
     * Remove um gênero existente do sistema.
     *
     * @param id O ID do gênero a ser removido.
     */
    void execute(Long id);
}