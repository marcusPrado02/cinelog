package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;

/**
 * Caso de uso para obtenção de pessoas.
 * Define a operação de recuperação de uma pessoa existente no sistema,
 * identificando-a pelo seu ID.
 */
public interface GetPersonUseCase {
    /**
     * Recupera uma pessoa existente do sistema.
     *
     * @param id O ID da pessoa a ser recuperada.
     * @return A pessoa correspondente ao ID fornecido.
     */
    Person execute(Long id);
}