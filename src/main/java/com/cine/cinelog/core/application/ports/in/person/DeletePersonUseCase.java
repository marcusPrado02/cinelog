package com.cine.cinelog.core.application.ports.in.person;

/**
 * Caso de uso para exclusão de pessoas.
 * Define a operação de remoção de uma pessoa existente no sistema,
 * identificando-a pelo seu ID.
 */
public interface DeletePersonUseCase {
    /**
     * Remove uma pessoa existente do sistema.
     *
     * @param id O ID da pessoa a ser removida.
     */
    void execute(Long id);
}
