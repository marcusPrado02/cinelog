package com.cine.cinelog.core.application.ports.in.genre;

import com.cine.cinelog.core.domain.model.Genre;

/**
 * Caso de uso para obtenção de gêneros.
 * Define a operação de recuperação de um gênero existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface GetGenreUseCase {
    /**
     * Recupera um gênero existente do sistema.
     *
     * @param id O ID do gênero a ser recuperado.
     * @return O gênero correspondente ao ID fornecido.
     */
    Genre execute(Long id);
}
