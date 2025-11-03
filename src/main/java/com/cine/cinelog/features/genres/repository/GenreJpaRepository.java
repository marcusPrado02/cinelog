package com.cine.cinelog.features.genres.repository;

import com.cine.cinelog.features.genres.persistence.entity.GenreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreJpaRepository extends JpaRepository<GenreEntity, Long> {
}