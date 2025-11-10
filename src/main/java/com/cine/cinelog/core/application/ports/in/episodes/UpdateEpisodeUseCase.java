package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;

/**
 * Caso de uso para atualização de episódios.
 * Define a operação de modificação de um episódio existente no sistema,
 * identificando-o pelo seu ID e recebendo os novos dados do episódio.
 * Retorna o episódio atualizado.
 */
public interface UpdateEpisodeUseCase {
    /**
     * Atualiza um episódio existente no sistema.
     *
     * @param id      O ID do episódio a ser atualizado.
     * @param episode O objeto Episode com os novos dados do episódio.
     * @return O episódio atualizado.
     */
    Episode execute(Long id, Episode episode);
}
