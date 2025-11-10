package com.cine.cinelog.features.watchentry.repository;

import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface WatchEntryJpaRepository extends JpaRepository<WatchEntryEntity, Long> {

  @Query("""
      SELECT w FROM WatchEntryEntity w
      WHERE w.userId = :userId
        AND (:mediaId IS NULL OR w.mediaId = :mediaId)
        AND (:episodeId IS NULL OR w.episodeId = :episodeId)
        AND (:minRating IS NULL OR w.rating >= :minRating)
        AND (:fromDate IS NULL OR w.watchedAt >= :fromDate)
        AND (:toDate IS NULL OR w.watchedAt <= :toDate)
      """)
  Page<WatchEntryEntity> search(Long userId, Long mediaId, Long episodeId, Integer minRating,
      LocalDate fromDate, LocalDate toDate, Pageable pageable);
}