package com.cine.cinelog.core.application.ports.in.season;

import com.cine.cinelog.core.domain.model.Season;
import java.util.List;

/**
 * Caso de uso para listagem de temporadas.
 * Define a operação de recuperação de todas as temporadas
 * disponíveis no sistema.
 * Retorna uma lista de objetos Season.
 */
public interface ListSeasonsUseCase {
    /**
     * Recupera todas as temporadas existentes do sistema.
     *
     * @return Uma lista de temporadas disponíveis.
     */
    List<Season> execute();
}
