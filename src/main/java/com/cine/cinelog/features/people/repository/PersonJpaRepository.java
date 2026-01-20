package com.cine.cinelog.features.people.repository;

import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}