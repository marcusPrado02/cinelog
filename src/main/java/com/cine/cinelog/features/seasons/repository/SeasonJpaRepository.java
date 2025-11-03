package com.cine.cinelog.features.seasons.repository;

import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonJpaRepository extends JpaRepository<SeasonEntity, Long> {
    List<SeasonEntity> findByMediaIdOrderBySeasonNumberAsc(Long mediaId);
}