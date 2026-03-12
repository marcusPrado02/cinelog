package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Genre;
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
    PageResult<Person> findAll(PageQuery query);

    /**
     * Remove uma pessoa existente do repositório.
     *
     * @param id O ID da pessoa a ser removida.
     */
    void deleteById(Long id);

    /**
     * Busca uma pessoa pelo identificador do TMDB.
     *
     * @param tmdbPersonId o ID da pessoa no TMDB
     * @return Optional com a pessoa encontrada, ou vazio
     */
    Optional<Person> findByTmdbPersonId(Long tmdbPersonId);

    /**
     * Busca uma pessoa pelo nome exato.
     *
     * @param name nome da pessoa
     * @return Optional com a pessoa encontrada, ou vazio
     */
    Optional<Person> findByName(String name);
}
