package com.cine.cinelog.core.application.ports.in.media;

import com.cine.cinelog.core.domain.model.Media;

/**
 * Caso de uso para recuperar uma mídia por seu identificador.
 *
 * Retorna a entidade de domínio `Media` correspondente ao `id` informado.
 * Deve lançar exceção ou retornar null/optional conforme implementação do
 * serviço
 * de aplicação, porém a porta define apenas o contrato.
 */
public interface GetMediaUseCase {
    /**
     * Recupera uma mídia existente do sistema.
     *
     * @param id O ID da mídia a ser recuperada.
     * @return A mídia correspondente ao ID fornecido.
     */
    Media execute(Long id);
}