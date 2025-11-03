package com.cine.cinelog.core.application.ports.in.credits;

import com.cine.cinelog.core.domain.model.Credit;

/**
 * Caso de uso para atualização de créditos.
 * Define a operação de modificação de um crédito existente no sistema,
 * identificando-o pelo seu ID e recebendo os novos dados do crédito.
 * Retorna o crédito atualizado.
 */
public interface UpdateCreditUseCase {
    Credit execute(Long id, Credit credit);
}