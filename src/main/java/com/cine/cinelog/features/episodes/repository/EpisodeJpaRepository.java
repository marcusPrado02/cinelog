package com.cine.cinelog.features.episodes.repository;

import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeJpaRepository extends JpaRepository<EpisodeEntity, Long> {
    List<EpisodeEntity> findBySeasonIdOrderByEpisodeNumberAsc(Long seasonId);
}
