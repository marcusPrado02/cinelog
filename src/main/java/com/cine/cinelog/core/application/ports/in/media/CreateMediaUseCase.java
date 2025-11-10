package com.cine.cinelog.core.application.ports.in.media;

import com.cine.cinelog.core.domain.model.Media;

/**
 * Caso de uso para criação de uma nova mídia.
 *
 * Recebe um objeto de domínio `Media` com os dados a serem persistidos e
 * devolve a entidade criada (normalmente com o `id` gerado).
 */
public interface CreateMediaUseCase {
    /**
     * Cria uma nova mídia no sistema.
     *
     * @param input O objeto Media com os dados da mídia a ser criada.
     * @return A mídia criada.
     */
    Media execute(Media input);
}