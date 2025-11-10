package com.cine.cinelog.core.application.ports.in.credits;

/**
 * Caso de uso para exclusão de créditos.
 * Define a operação de remoção de um crédito existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface DeleteCreditUseCase {
    /**
     * Remove um crédito existente do sistema.
     *
     * @param id O ID do crédito a ser removido.
     */
    void execute(Long id);
}