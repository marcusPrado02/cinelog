package com.cine.cinelog.core.application.ports.in.genre;

import com.cine.cinelog.core.domain.model.Genre;

/**
 * Caso de uso para criação de gêneros.
 * Define a operação de criação de um novo gênero no sistema,
 * recebendo um objeto Genre e retornando o gênero criado.
 */
public interface CreateGenreUseCase {
    /**
     * Cria um novo gênero no sistema.
     *
     * @param genre O objeto Genre com os dados do gênero a ser criado.
     * @return O gênero criado.
     */
    Genre execute(Genre genre);
}
