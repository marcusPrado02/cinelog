package com.cine.cinelog.features.genres.repository;

import com.cine.cinelog.features.genres.persistence.entity.GenreEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para gerenciamento de gêneros de mídias.
 * 
 * <p>
 * Fornece operações CRUD básicas herdadas de JpaRepository para
 * {@link GenreEntity} (Ação, Drama, Comédia, etc.).
 * 
 * @since 1.0
 * @see GenreEntity
 */
public interface GenreJpaRepository extends JpaRepository<GenreEntity, Long> {

    /**
     * Busca um gênero pelo nome.
     *
     * @param name O nome do gênero a ser buscado.
     * @return Optional contendo o gênero se encontrado.
     */
    Optional<GenreEntity> findByName(String name);
}