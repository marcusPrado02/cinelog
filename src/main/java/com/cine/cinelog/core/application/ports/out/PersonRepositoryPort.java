package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Person;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de repositório relacionadas a pessoas.
 * Define as operações CRUD básicas para gerenciar entidades Person.
 */
public interface PersonRepositoryPort {
    /**
     * Salva uma pessoa no repositório.
     *
     * @param person O objeto Person a ser salvo.
     * @return A pessoa salva.
     */
    Person save(Person person);

    /**
     * Recupera uma pessoa existente do repositório.
     *
     * @param id O ID da pessoa a ser recuperada.
     * @return Um Optional contendo a pessoa encontrada, ou vazio se não
     *         encontrado.
     */
    Optional<Person> findById(Long id);

    /**
     * Recupera todas as pessoas existentes do repositório.
     *
     * @return Uma lista de pessoas encontradas.
     */
    List<Person> findAll();

    /**
     * Remove uma pessoa existente do repositório.
     *
     * @param id O ID da pessoa a ser removida.
     */
    void deleteById(Long id);
}
