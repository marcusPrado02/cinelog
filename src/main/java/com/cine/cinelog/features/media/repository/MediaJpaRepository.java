package com.cine.cinelog.features.media.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.MediaWithRating;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.projection.MediaWithRatingProjection;

/**
 * Repositório Spring Data JPA com consultas de paginação usadas pelo
 * adapter de persistência.
 */
public interface MediaJpaRepository extends JpaRepository<MediaEntity, Long>, JpaSpecificationExecutor<MediaEntity> {
  Page<MediaEntity> findByType(MediaType type, Pageable pageable);

  Page<MediaEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);

  Page<MediaEntity> findByTypeAndTitleContainingIgnoreCase(MediaType type, String title, Pageable pageable);

  /**
   * Retorna uma lista de mídias "candidatas" a recomendação
   * (por exemplo, mais assistidas ou por gênero do usuário).
   */
  @Query("""
      SELECT m.id AS mediaId,
             m.title AS title,
             m.type AS type,
             AVG(w.rating) AS averageRating,
             COUNT(w.id) AS ratingCount
      FROM MediaEntity m
      JOIN WatchEntryEntity w ON m.id = w.mediaId
      WHERE w.rating IS NOT NULL
        AND m.id NOT IN (
          SELECT wi.mediaId
          FROM WatchlistItemEntity wi
          WHERE wi.userId = :userId
      )
      GROUP BY m.id, m.title, m.type
      HAVING COUNT(w.id) >= 5
      ORDER BY averageRating DESC
      """)
  List<MediaWithRatingProjection> findCandidatesForUser(Long userId);

  /**
   * Busca uma mídia pelo ID do TMDB.
   */
  Optional<MediaEntity> findByTmdbId(Long tmdbId);

  /**
   * Retorna todas as mídias de um tipo específico que possuem tmdbId definido.
   */
  List<MediaEntity> findByTypeAndTmdbIdNotNull(MediaType type);

  /**
   * Retorna mídias com tmdbId definido mas sem posterUrl ou backdropUrl.
   * Usadas pelo job de enriquecimento de imagens.
   */
  @Query("SELECT m FROM MediaEntity m WHERE m.tmdbId IS NOT NULL AND (m.posterUrl IS NULL OR m.backdropUrl IS NULL)")
  List<MediaEntity> findAllMissingImages();

  @Query("SELECT m FROM MediaEntity m WHERE m.tmdbId IS NULL")
  List<MediaEntity> findAllWithoutTmdbId();
}
