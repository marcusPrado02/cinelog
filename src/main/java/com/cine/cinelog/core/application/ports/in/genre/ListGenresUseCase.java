package com.cine.cinelog.core.application.ports.in.genre;

import com.cine.cinelog.core.domain.model.Genre;
import java.util.List;

/**
 * Caso de uso para listagem de gêneros.
 * Define a operação de recuperação de todos os gêneros disponíveis no
 * sistema.
 * Retorna uma lista de objetos Genre.
 */
public interface ListGenresUseCase {
    /**
     * Recupera todos os gêneros existentes do sistema.
     *
     * @return Uma lista de gêneros disponíveis.
     */
    List<Genre> execute();
}