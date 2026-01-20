package com.cine.cinelog.features.readmodels.repository;

import com.cine.cinelog.features.readmodels.persistence.entity.MediaPopularityEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório JPA para media_popularity (CQRS Read Model).
 *
 * <p>
 * Fornece operações de persistência otimizadas para queries de popularidade:
 * <ul>
 * <li>Top Rated (por avg_rating)</li>
 * <li>Trending (por watch_count + recência)</li>
 * <li>Most Watched (por watch_count total)</li>
 * </ul>
 *
 * <p>
 * Esta tabela é atualizada de forma assíncrona por consumidores Kafka.
 *
 * @since 1.0 (PR6)
 * @see MediaPopularityEntity
 */
@Repository
public interface MediaPopularityRepository extends JpaRepository<MediaPopularityEntity, Long> {

    /**
     * Retorna top N mídias com maior média de rating.
     * Usado para feature "Top Rated".
     *
     * @param pageable paginação (usar PageRequest.of(0, limit))
     * @return lista de mídias ordenada por avg_rating DESC
     */
    @Query("""
            SELECT m FROM MediaPopularityEntity m
            WHERE m.avgRating IS NOT NULL
            AND m.ratingsCount >= 3
            ORDER BY m.avgRating DESC, m.ratingsCount DESC
            """)
    List<MediaPopularityEntity> findTopRated(Pageable pageable);

    /**
     * Retorna mídias mais assistidas de todos os tempos.
     *
     * @param pageable paginação
     * @return lista de mídias ordenada por watch_count DESC
     */
    @Query("""
            SELECT m FROM MediaPopularityEntity m
            WHERE m.watchCount > 0
            ORDER BY m.watchCount DESC
            """)
    List<MediaPopularityEntity> findMostWatched(Pageable pageable);

    /**
     * Retorna mídias trending (muitas visualizações recentes).
     * Combina watch_count com recência.
     *
     * @param since    data mínima para considerar (ex: 7 dias atrás)
     * @param pageable paginação
     * @return lista de mídias ordenada por watch_count DESC, last_watched_at DESC
     */
    @Query("""
            SELECT m FROM MediaPopularityEntity m
            WHERE m.lastWatchedAt >= :since
            AND m.watchCount > 0
            ORDER BY m.watchCount DESC, m.lastWatchedAt DESC
            """)
    List<MediaPopularityEntity> findTrending(LocalDateTime since, Pageable pageable);

    /**
     * Retorna mídias mais recentemente assistidas.
     *
     * @param pageable paginação
     * @return lista de mídias ordenada por last_watched_at DESC
     */
    @Query("""
            SELECT m FROM MediaPopularityEntity m
            WHERE m.lastWatchedAt IS NOT NULL
            ORDER BY m.lastWatchedAt DESC
            """)
    List<MediaPopularityEntity> findRecentlyWatched(Pageable pageable);
}
