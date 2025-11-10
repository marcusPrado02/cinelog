package com.cine.cinelog.core.application.ports.in.media;

/**
 * Caso de uso para exclusão de mídias.
 * Define a operação de remoção de uma mídia existente no sistema,
 * identificando-a pelo seu ID.
 */
public interface DeleteMediaUseCase {
    /**
     * Remove uma mídia existente do sistema.
     *
     * @param id O ID da mídia a ser removida.
     */
    void execute(Long id);
}
