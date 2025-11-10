package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;

/**
 * Caso de uso para criação de episódios.
 * Define a operação de criação de um novo episódio no sistema,
 * recebendo um objeto Episode e retornando o episódio criado.
 */
public interface CreateEpisodeUseCase {
    /**
     * Cria um novo episódio no sistema.
     *
     * @param episode O objeto Episode com os dados do episódio a ser criado.
     * @return O episódio criado.
     */
    Episode execute(Episode episode);
}
