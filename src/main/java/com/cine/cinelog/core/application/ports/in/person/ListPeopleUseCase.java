package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;
import java.util.List;

/**
 * Caso de uso para listagem de pessoas.
 * Define a operação de recuperação de todas as pessoas
 * disponíveis no sistema.
 * Retorna uma lista de objetos Person.
 */
public interface ListPeopleUseCase {
    /**
     * Recupera uma lista de pessoas existentes do sistema.
     *
     * @return Uma lista de pessoas que atendem aos critérios.
     */
    List<Person> execute();
}