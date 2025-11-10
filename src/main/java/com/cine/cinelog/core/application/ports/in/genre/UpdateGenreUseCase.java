package com.cine.cinelog.core.application.ports.in.genre;

import com.cine.cinelog.core.domain.model.Genre;

/**
 * Caso de uso para atualização de gêneros.
 * Define a operação de modificação de um gênero existente no sistema,
 * identificando-o pelo seu ID e recebendo um objeto Genre com os
 * dados atualizados.
 */
public interface UpdateGenreUseCase {
    /**
     * Atualiza um gênero existente no sistema.
     *
     * @param id    O ID do gênero a ser atualizado.
     * @param genre O objeto Genre com os novos dados do gênero.
     * @return O gênero atualizado.
     */
    Genre execute(Long id, Genre genre);
}