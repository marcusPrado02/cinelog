package com.cine.cinelog.core.application.ports.in.media;

import com.cine.cinelog.core.domain.model.Media;

/**
 * Caso de uso para atualização de mídias.
 * Define a operação de modificação de uma mídia existente no sistema,
 * identificando-a pelo seu ID e recebendo um objeto Media com os
 * dados atualizados.
 */
public interface UpdateMediaUseCase {
    /**
     * Atualiza uma mídia existente no sistema.
     *
     * @param id   O ID da mídia a ser atualizada.
     * @param data O objeto Media com os novos dados da mídia.
     * @return A mídia atualizada.
     */
    Media execute(Long id, Media data);
}
