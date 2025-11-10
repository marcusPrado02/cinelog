package com.cine.cinelog.core.application.ports.in.season;

import com.cine.cinelog.core.domain.model.Season;

/**
 * Caso de uso para obtenção de temporadas.
 * Define a operação de recuperação de uma temporada existente no sistema,
 * identificando-a pelo seu ID.
 */
public interface GetSeasonUseCase {
    /**
     * Recupera uma temporada existente do sistema.
     *
     * @param id O ID da temporada a ser recuperada.
     * @return A temporada correspondente ao ID fornecido.
     */
    Season execute(Long id);
}