package com.cine.cinelog.features.media.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;

/**
 * Repositório Spring Data JPA com consultas de paginação usadas pelo
 * adapter de persistência.
 */
public interface MediaJpaRepository extends JpaRepository<MediaEntity, Long> {
    Page<MediaEntity> findByType(MediaType type, Pageable pageable);

    Page<MediaEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<MediaEntity> findByTypeAndTitleContainingIgnoreCase(MediaType type, String title, Pageable pageable);
}