package com.cine.cinelog.core.application.ports.in.season;

import com.cine.cinelog.core.domain.model.Season;

/**
 * Caso de uso para atualização de temporadas.
 * Define a operação de modificação de uma temporada existente no sistema,
 * identificando-a pelo seu ID e recebendo um objeto Season com os
 * dados atualizados.
 */
public interface UpdateSeasonUseCase {
    /**
     * Atualiza uma temporada existente no sistema.
     *
     * @param id     O ID da temporada a ser atualizada.
     * @param season O objeto Season com os novos dados da temporada.
     * @return A temporada atualizada.
     */
    Season execute(Long id, Season season);
}
