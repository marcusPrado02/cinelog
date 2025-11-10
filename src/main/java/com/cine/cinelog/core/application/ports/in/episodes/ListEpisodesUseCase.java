package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;
import java.util.List;

/**
 * Caso de uso para listagem de episódios.
 * Define a operação de recuperação de todos os episódios disponíveis no
 * sistema.
 * Retorna uma lista de objetos Episode.
 */
public interface ListEpisodesUseCase {
    /**
     * Recupera todos os episódios existentes do sistema.
     *
     * @return Uma lista de episódios disponíveis.
     */
    List<Episode> execute();
}
