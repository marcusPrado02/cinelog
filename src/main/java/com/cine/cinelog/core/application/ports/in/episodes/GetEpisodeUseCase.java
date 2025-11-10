package com.cine.cinelog.core.application.ports.in.episodes;

import com.cine.cinelog.core.domain.model.Episode;

/**
 * Caso de uso para obtenção de episódios.
 * Define a operação de recuperação de um episódio específico pelo seu ID.
 * Retorna o objeto Episode correspondente.
 */
public interface GetEpisodeUseCase {
    /**
     * Recupera um episódio existente do sistema.
     *
     * @param id O ID do episódio a ser recuperado.
     * @return O episódio correspondente ao ID fornecido.
     */
    Episode execute(Long id);
}
