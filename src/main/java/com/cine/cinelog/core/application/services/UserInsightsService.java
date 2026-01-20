package com.cine.cinelog.core.application.services;

import com.cine.cinelog.features.readmodels.persistence.entity.UserStatsEntity;
import com.cine.cinelog.features.readmodels.repository.UserStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para consultar estatísticas e insights de usuários.
 *
 * <p>
 * Este serviço consulta o read model {@link UserStatsEntity}, que é atualizado
 * de forma assíncrona por consumidores Kafka (eventual consistency).
 *
 * <p>
 * <strong>Features:</strong>
 * <ul>
 * <li>GET /api/users/{userId}/insights: estatísticas completas do usuário</li>
 * <li>Dados cached para melhor performance</li>
 * <li>Eventual consistency: pode haver delay entre write e read</li>
 * </ul>
 *
 * @since 1.0 (PR6)
 * @see UserStatsEntity
 * @see com.cine.cinelog.infrastructure.messaging.kafka.consumer.UserStatsUpdater
 */
@Service
@Transactional(readOnly = true)
public class UserInsightsService {

    private static final Logger log = LoggerFactory.getLogger(UserInsightsService.class);

    private final UserStatsRepository userStatsRepository;

    public UserInsightsService(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    /**
     * Retorna insights completos de um usuário.
     *
     * <p>
     * Resultado é cached por 5 minutos. Para forçar refresh, invalidar cache
     * ou aguardar TTL expirar.
     *
     * @param userId ID do usuário
     * @return mapa com estatísticas (totalWatched, totalMovies, totalSeries,
     *         avgRating, lastWatchedAt)
     * @throws IllegalArgumentException se userId for null
     */
    @Cacheable(value = "userInsights", key = "#userId", unless = "#result == null")
    public Map<String, Object> getUserInsights(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        log.debug("Buscando insights para userId={}", userId);

        UserStatsEntity stats = userStatsRepository.findById(userId)
                .orElse(new UserStatsEntity(userId));

        Map<String, Object> insights = new HashMap<>();
        insights.put("userId", stats.getUserId());
        insights.put("totalWatched", stats.getTotalWatched());
        insights.put("totalMovies", stats.getTotalMovies());
        insights.put("totalSeries", stats.getTotalSeries());
        insights.put("avgRating", stats.getAvgRating());
        insights.put("lastWatchedAt", stats.getLastWatchedAt());
        insights.put("updatedAt", stats.getUpdatedAt());

        log.debug("Insights retornados: totalWatched={}, avgRating={}",
                stats.getTotalWatched(), stats.getAvgRating());

        return insights;
    }

    /**
     * Retorna se o usuário tem estatísticas registradas.
     *
     * @param userId ID do usuário
     * @return true se existe registro, false caso contrário
     */
    public boolean hasStats(Long userId) {
        if (userId == null) {
            return false;
        }
        return userStatsRepository.existsById(userId);
    }

    /**
     * Retorna rating médio de um usuário.
     *
     * @param userId ID do usuário
     * @return média de rating ou null se não houver dados
     */
    public BigDecimal getAverageRating(Long userId) {
        if (userId == null) {
            return null;
        }

        return userStatsRepository.findById(userId)
                .map(UserStatsEntity::getAvgRating)
                .orElse(null);
    }

    /**
     * Retorna data da última visualização de um usuário.
     *
     * @param userId ID do usuário
     * @return data da última visualização ou null se não houver dados
     */
    public LocalDate getLastWatchedDate(Long userId) {
        if (userId == null) {
            return null;
        }

        return userStatsRepository.findById(userId)
                .map(UserStatsEntity::getLastWatchedAt)
                .orElse(null);
    }
}
