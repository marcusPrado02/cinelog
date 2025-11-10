package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;

/**
 * Caso de uso para atualização de pessoas.
 * Define a operação de modificação de uma pessoa existente no sistema,
 * identificando-a pelo seu ID e recebendo um objeto Person com os
 * dados atualizados.
 */
public interface UpdatePersonUseCase {
    /**
     * Atualiza uma pessoa existente no sistema.
     *
     * @param id     O ID da pessoa a ser atualizada.
     * @param person O objeto Person com os novos dados da pessoa.
     * @return A pessoa atualizada.
     */
    Person execute(Long id, Person person);
}