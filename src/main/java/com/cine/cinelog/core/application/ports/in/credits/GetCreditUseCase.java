package com.cine.cinelog.core.application.ports.in.credits;

import com.cine.cinelog.core.domain.model.Credit;

/**
 * Caso de uso para obtenção de créditos.
 * Define a operação de recuperação de um crédito específico pelo seu ID.
 * Retorna o objeto Credit correspondente.
 */
public interface GetCreditUseCase {
    /**
     * Recupera um crédito existente do sistema.
     *
     * @param id O ID do crédito a ser recuperado.
     * @return O crédito correspondente ao ID fornecido.
     */
    Credit execute(Long id);
}