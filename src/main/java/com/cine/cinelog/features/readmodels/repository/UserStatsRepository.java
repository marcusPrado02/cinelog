package com.cine.cinelog.features.readmodels.repository;

import com.cine.cinelog.features.readmodels.persistence.entity.UserStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório JPA para user_stats (CQRS Read Model).
 *
 * <p>
 * Fornece operações de persistência otimizadas para consultas de estatísticas
 * de usuário.
 * Esta tabela é atualizada de forma assíncrona por consumidores Kafka.
 *
 * @since 1.0 (PR6)
 * @see UserStatsEntity
 */
@Repository
public interface UserStatsRepository extends JpaRepository<UserStatsEntity, Long> {

    /**
     * Retorna top N usuários com maior número de visualizações.
     *
     * @param limit número máximo de resultados
     * @return lista de usuários ordenada por total_watched DESC
     */
    @Query("""
            SELECT u FROM UserStatsEntity u
            WHERE u.totalWatched > 0
            ORDER BY u.totalWatched DESC
            LIMIT :limit
            """)
    List<UserStatsEntity> findTopByTotalWatched(int limit);

    /**
     * Retorna top N usuários com maior média de rating.
     *
     * @param limit número máximo de resultados
     * @return lista de usuários ordenada por avg_rating DESC
     */
    @Query("""
            SELECT u FROM UserStatsEntity u
            WHERE u.avgRating IS NOT NULL
            ORDER BY u.avgRating DESC
            LIMIT :limit
            """)
    List<UserStatsEntity> findTopByAvgRating(int limit);

    /**
     * Retorna usuários mais ativos recentemente.
     *
     * @param limit número máximo de resultados
     * @return lista de usuários ordenada por last_watched_at DESC
     */
    @Query("""
            SELECT u FROM UserStatsEntity u
            WHERE u.lastWatchedAt IS NOT NULL
            ORDER BY u.lastWatchedAt DESC
            LIMIT :limit
            """)
    List<UserStatsEntity> findMostActiveRecent(int limit);
}
