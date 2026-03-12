package com.cine.cinelog.features.people.repository;

import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA para gerenciamento de pessoas (profissionais do cinema/TV).
 *
 * <p>
 * Fornece operações de persistência para {@link PersonEntity}, incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca por nome com paginação</li>
 * </ul>
 *
 * @since 1.0
 * @see PersonEntity
 */
public interface PersonJpaRepository extends JpaRepository<PersonEntity, Long> {
    /**
     * Busca pessoas cujo nome contém o termo especificado (case-insensitive).
     *
     * @param name o termo de busca
     * @param pg   parâmetros de paginação
     * @return página de pessoas encontradas
     */
    Page<PersonEntity> findByNameContainingIgnoreCase(String name, Pageable pg);

    /**
     * Busca uma pessoa pelo identificador TMDB.
     *
     * @param tmdbPersonId identificador no TMDB
     * @return Optional com a pessoa encontrada, ou vazio
     */
    Optional<PersonEntity> findByTmdbPersonId(Long tmdbPersonId);

    /**
     * Busca uma pessoa pelo nome exato (case-insensitive).
     *
     * @param name nome da pessoa
     * @return Optional com a pessoa encontrada, ou vazio
     */
    Optional<PersonEntity> findByNameIgnoreCase(String name);
}
