package com.cine.cinelog.core.application.services;

import com.cine.cinelog.features.readmodels.persistence.entity.MediaPopularityEntity;
import com.cine.cinelog.features.readmodels.repository.MediaPopularityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço para consultar popularidade e tendências de mídias.
 *
 * <p>
 * Este serviço consulta o read model {@link MediaPopularityEntity}, que é
 * atualizado
 * de forma assíncrona por consumidores Kafka (eventual consistency).
 *
 * <p>
 * <strong>Features:</strong>
 * <ul>
 * <li>Top Rated: mídias com maior média de rating (min 3 avaliações)</li>
 * <li>Trending: mídias com mais visualizações recentes (últimos 7/30 dias)</li>
 * <li>Most Watched: mídias com mais visualizações totais</li>
 * </ul>
 *
 * <p>
 * <strong>Performance:</strong> Queries otimizadas com índices em:
 * <ul>
 * <li>avg_rating DESC</li>
 * <li>watch_count DESC</li>
 * <li>last_watched_at DESC</li>
 * <li>(watch_count, last_watched_at) DESC - índice composto</li>
 * </ul>
 *
 * @since 1.0 (PR6)
 * @see MediaPopularityEntity
 * @see com.cine.cinelog.infrastructure.messaging.kafka.consumer.MediaPopularityUpdater
 */
@Service
@Transactional(readOnly = true)
public class MediaPopularityService {

    private static final Logger log = LoggerFactory.getLogger(MediaPopularityService.class);

    private static final int MIN_RATINGS_FOR_TOP_RATED = 3;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final MediaPopularityRepository mediaPopularityRepository;

    public MediaPopularityService(MediaPopularityRepository mediaPopularityRepository) {
        this.mediaPopularityRepository = mediaPopularityRepository;
    }

    /**
     * Retorna top N mídias com maior média de rating.
     *
     * <p>
     * Critérios:
     * <ul>
     * <li>Mínimo de 3 avaliações (evitar ratings inflados)</li>
     * <li>Ordenado por avg_rating DESC, ratings_count DESC</li>
     * </ul>
     *
     * <p>
     * Resultado é cached por 10 minutos.
     *
     * @param limit número máximo de resultados (default: 20, max: 100)
     * @return lista de mídias ordenada por rating
     */
    @Cacheable(value = "topRatedMedia", key = "#limit")
    public List<MediaPopularityEntity> getTopRated(int limit) {
        int validatedLimit = validateLimit(limit);

        log.debug("Buscando top {} mídias mais bem avaliadas (min {} ratings)",
                validatedLimit, MIN_RATINGS_FOR_TOP_RATED);

        Pageable pageable = PageRequest.of(0, validatedLimit);
        List<MediaPopularityEntity> topRated = mediaPopularityRepository.findTopRated(pageable);

        log.debug("Top rated retornou {} mídias", topRated.size());

        return topRated;
    }

    /**
     * Retorna mídias trending (mais assistidas recentemente).
     *
     * <p>
     * Períodos suportados:
     * <ul>
     * <li>WEEK: últimos 7 dias</li>
     * <li>MONTH: últimos 30 dias</li>
     * <li>ALL_TIME: desde sempre</li>
     * </ul>
     *
     * <p>
     * Ordenado por watch_count DESC, last_watched_at DESC.
     *
     * @param period período para considerar (WEEK, MONTH, ALL_TIME)
     * @param limit  número máximo de resultados
     * @return lista de mídias trending
     */
    @Cacheable(value = "trendingMedia", key = "#period + '_' + #limit")
    public List<MediaPopularityEntity> getTrending(TrendingPeriod period, int limit) {
        int validatedLimit = validateLimit(limit);

        LocalDateTime since = calculateSinceDate(period);

        log.debug("Buscando top {} mídias trending (período: {}, desde: {})",
                validatedLimit, period, since);

        Pageable pageable = PageRequest.of(0, validatedLimit);
        List<MediaPopularityEntity> trending = mediaPopularityRepository.findTrending(since, pageable);

        log.debug("Trending retornou {} mídias", trending.size());

        return trending;
    }

    /**
     * Retorna mídias mais assistidas de todos os tempos.
     *
     * @param limit número máximo de resultados
     * @return lista de mídias ordenada por watch_count DESC
     */
    @Cacheable(value = "mostWatchedMedia", key = "#limit")
    public List<MediaPopularityEntity> getMostWatched(int limit) {
        int validatedLimit = validateLimit(limit);

        log.debug("Buscando top {} mídias mais assistidas", validatedLimit);

        Pageable pageable = PageRequest.of(0, validatedLimit);
        List<MediaPopularityEntity> mostWatched = mediaPopularityRepository.findMostWatched(pageable);

        log.debug("Most watched retornou {} mídias", mostWatched.size());

        return mostWatched;
    }

    /**
     * Retorna mídias mais recentemente assistidas.
     *
     * @param limit número máximo de resultados
     * @return lista de mídias ordenada por last_watched_at DESC
     */
    @Cacheable(value = "recentlyWatchedMedia", key = "#limit")
    public List<MediaPopularityEntity> getRecentlyWatched(int limit) {
        int validatedLimit = validateLimit(limit);

        log.debug("Buscando {} mídias assistidas recentemente", validatedLimit);

        Pageable pageable = PageRequest.of(0, validatedLimit);
        List<MediaPopularityEntity> recentlyWatched = mediaPopularityRepository.findRecentlyWatched(pageable);

        log.debug("Recently watched retornou {} mídias", recentlyWatched.size());

        return recentlyWatched;
    }

    /**
     * Valida e ajusta o limit para range permitido.
     */
    private int validateLimit(int limit) {
        if (limit <= 0) {
            log.warn("Limit inválido: {}. Usando default: {}", limit, DEFAULT_LIMIT);
            return DEFAULT_LIMIT;
        }

        if (limit > MAX_LIMIT) {
            log.warn("Limit excede máximo: {}. Usando max: {}", limit, MAX_LIMIT);
            return MAX_LIMIT;
        }

        return limit;
    }

    /**
     * Calcula data mínima baseada no período trending.
     */
    private LocalDateTime calculateSinceDate(TrendingPeriod period) {
        LocalDateTime now = LocalDateTime.now();

        return switch (period) {
            case WEEK -> now.minusDays(7);
            case MONTH -> now.minusDays(30);
            case ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0); // Arbitrário: ano 2000
        };
    }

    /**
     * Enum representando períodos para trending queries.
     */
    public enum TrendingPeriod {
        WEEK,
        MONTH,
        ALL_TIME
    }
}
