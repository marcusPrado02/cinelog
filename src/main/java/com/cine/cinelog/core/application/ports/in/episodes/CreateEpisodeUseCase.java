package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;

/**
 * Caso de uso para criação de episódios.
 * Define a operação de criação de um novo episódio no sistema,
 * recebendo um objeto Episode e retornando o episódio criado.
 */
public interface CreateEpisodeUseCase {
    Episode execute(Episode episode);
}
