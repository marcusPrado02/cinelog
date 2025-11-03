package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;

/**
 * Caso de uso para obtenção de episódios.
 * Define a operação de recuperação de um episódio específico pelo seu ID.
 * Retorna o objeto Episode correspondente.
 */
public interface GetEpisodeUseCase {
    Episode execute(Long id);
}
