package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;

/**
 * Caso de uso para atualização de episódios.
 * Define a operação de modificação de um episódio existente no sistema,
 * identificando-o pelo seu ID e recebendo os novos dados do episódio.
 * Retorna o episódio atualizado.
 */
public interface UpdateEpisodeUseCase {
    Episode execute(Long id, Episode episode);
}
