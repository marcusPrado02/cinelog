package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;

/**
 * Caso de uso para criação de pessoas.
 * Define a operação de inserção de uma nova pessoa no sistema,
 * recebendo um objeto Person com os dados da pessoa a ser criada.
 */
public interface CreatePersonUseCase {
    /**
     * Cria uma nova pessoa no sistema.
     *
     * @param person O objeto Person com os dados da pessoa a ser criada.
     * @return A pessoa criada.
     */
    Person execute(Person person);
}