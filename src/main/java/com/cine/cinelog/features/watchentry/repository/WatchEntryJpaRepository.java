package com.cine.cinelog.features.watchentry.repository;

import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;
import com.cine.cinelog.features.watchentry.persistence.projection.UserStatsProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositório JPA para gerenciamento de entradas de visualização (watch
 * entries).
 * 
 * <p>
 * Fornece operações de persistência para {@link WatchEntryEntity}, incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca avançada com filtros múltiplos</li>
 * <li>Cálculo de estatísticas de usuário</li>
 * <li>Verificação de existência e unicidade</li>
 * </ul>
 * 
 * @since 1.0
 * @see WatchEntryEntity
 * @see UserStatsProjection
 */
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

        @Query("""
                        select
                            we.userId as userId,
                            count(we.id) as totalEntries,
                            count(we.rating) as totalRated,
                            avg(we.rating) as averageRating,
                            min(we.watchedAt) as firstWatchDate,
                            max(we.watchedAt) as lastWatchDate
                        from WatchEntryEntity we
                        where we.userId = :userId
                        """)
        UserStatsProjection computeStatsForUser(Long userId);

        boolean existsByMediaId(Long mediaId);

        boolean existsByUserIdAndMediaIdAndEpisodeIdAndWatchedAt(
                        Long userId,
                        Long mediaId,
                        Long episodeId,
                        LocalDate watchedAt);

        boolean existsByUserId(Long userId);

        @Query("""
                        SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
                        FROM WatchEntryEntity w
                        WHERE w.episodeId = :episodeId
                        """)
        boolean existsByEpisodeId(Long episodeId);

        /**
         * Retorna total de entradas para o usuário.
         * 
         * @param userId O ID do usuário.
         * @return total de entradas.
         */
        @Query("""
                        SELECT COUNT(w)
                        FROM WatchEntryEntity w
                        WHERE w.userId = :userId
                        """)
        long countEntriesByUserId(Long userId);

        /**
         * Retorna total de entradas com rating (onde rating != null).
         * 
         * @param userId O ID do usuário.
         * @return total de entradas com rating.
         */
        @Query("""
                        SELECT COUNT(w)
                        FROM WatchEntryEntity w
                        WHERE w.userId = :userId AND w.rating IS NOT NULL
                        """)
        long countRatedEntriesByUserId(Long userId);

        /**
         * Retorna média dos ratings do usuário, se existir ao menos um.
         * 
         * @param userId O ID do usuário.
         * @return média dos ratings, ou vazio se não houver ratings.
         */
        @Query("""
                        SELECT AVG(w.rating)
                        FROM WatchEntryEntity w
                        WHERE w.userId = :userId AND w.rating IS NOT NULL
                        """)
        Optional<Double> averageRatingByUserId(Long userId);

        /**
         * Retorna a data da primeira visualização do usuário.
         * 
         * @param userId O ID do usuário.
         * @return A data da primeira visualização, ou null se não houver.
         */
        @Query("""
                        SELECT MIN(w.watchedAt)
                        FROM WatchEntryEntity w
                        WHERE w.userId = :userId
                        """)
        LocalDate findFirstWatchDateByUserId(Long userId);

        /**
         * Retorna a data da última visualização do usuário.
         *
         * @param userId O ID do usuário.
         * @return A data da última visualização, ou null se não houver.
         */
        @Query("""
                        SELECT MAX(w.watchedAt)
                        FROM WatchEntryEntity w
                        WHERE w.userId = :userId
                        """)
        LocalDate findLastWatchDateByUserId(Long userId);

}