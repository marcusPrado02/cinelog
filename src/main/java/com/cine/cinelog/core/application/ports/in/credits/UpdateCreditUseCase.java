package com.cine.cinelog.core.application.ports.in.credits;

import com.cine.cinelog.core.domain.model.Credit;

/**
 * Caso de uso para atualização de créditos.
 * Define a operação de modificação de um crédito existente no sistema,
 * identificando-o pelo seu ID e recebendo os novos dados do crédito.
 * Retorna o crédito atualizado.
 */
public interface UpdateCreditUseCase {
    /**
     * Atualiza um crédito existente no sistema.
     *
     * @param id     O ID do crédito a ser atualizado.
     * @param credit O objeto Credit com os novos dados do crédito.
     * @return O crédito atualizado.
     */
    Credit execute(Long id, Credit credit);
}