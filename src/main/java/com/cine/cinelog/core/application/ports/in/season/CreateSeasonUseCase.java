package com.cine.cinelog.core.application.ports.in.season;

import com.cine.cinelog.core.domain.model.Season;

/**
 * Caso de uso para criação de temporadas.
 * Define a operação de inserção de uma nova temporada no sistema,
 * recebendo um objeto Season com os dados da temporada a ser criada.
 */
public interface CreateSeasonUseCase {
    /**
     * Cria uma nova temporada no sistema.
     *
     * @param season O objeto Season com os dados da temporada a ser criada.
     * @return A temporada criada.
     */
    Season execute(Season season);
}
