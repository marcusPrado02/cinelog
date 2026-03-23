package com.cine.cinelog.shared.config.redis;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * CacheManager com TTLs explicitos por cache.
     *
     * <p>Categorias de TTL:</p>
     * <ul>
     *   <li><b>Catalogo (24h)</b>: genres, genresPage, genreById — dados que mudam raramente</li>
     *   <li><b>Media (2h)</b>: media, mediaById, mediaPage, mediaSearch — atualizados por TMDB sync</li>
     *   <li><b>CQRS read models (30min)</b>: topRatedMedia, trendingMedia, mostWatchedMedia,
     *       recentlyWatchedMedia — atualizados por Kafka consumers</li>
     *   <li><b>Recommendations (1h)</b>: algoritmo pesado, cache reduz recalculo</li>
     *   <li><b>Entities by ID (30min)</b>: creditById, personById, episodeById, seasonById,
     *       userById, watchEntryById — evict explicito em update/delete</li>
     *   <li><b>Entity pages (15min)</b>: creditsPage, peoplePage, episodesPage, seasonsPage,
     *       usersPage, watchlistPage, watchEntriesPage — evict allEntries em create/update/delete</li>
     *   <li><b>User data (5min)</b>: userStats, userInsights, watchProgress — refletem acoes recentes</li>
     *   <li><b>TMDB (6h)</b>: cache de chamadas externas ao TMDB API</li>
     * </ul>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.ofEntries(
                // ── Catalogo (24h) ──
                entry("genres", defaultConfig, Duration.ofHours(24)),
                entry("genresPage", defaultConfig, Duration.ofHours(24)),
                entry("genreById", defaultConfig, Duration.ofHours(24)),

                // ── Media (2h) ──
                entry("media", defaultConfig, Duration.ofHours(2)),
                entry("mediaById", defaultConfig, Duration.ofHours(2)),
                entry("mediaPage", defaultConfig, Duration.ofHours(2)),
                entry("mediaSearch", defaultConfig, Duration.ofHours(2)),

                // ── CQRS read models (30min) ──
                entry("topRatedMedia", defaultConfig, Duration.ofMinutes(30)),
                entry("trendingMedia", defaultConfig, Duration.ofMinutes(30)),
                entry("mostWatchedMedia", defaultConfig, Duration.ofMinutes(30)),
                entry("recentlyWatchedMedia", defaultConfig, Duration.ofMinutes(30)),

                // ── Recommendations (1h) ──
                entry("recommendations", defaultConfig, Duration.ofHours(1)),

                // ── Entities by ID (30min) ──
                entry("creditById", defaultConfig, Duration.ofMinutes(30)),
                entry("personById", defaultConfig, Duration.ofMinutes(30)),
                entry("episodeById", defaultConfig, Duration.ofMinutes(30)),
                entry("seasonById", defaultConfig, Duration.ofMinutes(30)),
                entry("userById", defaultConfig, Duration.ofMinutes(30)),
                entry("watchEntryById", defaultConfig, Duration.ofMinutes(30)),

                // ── Entity pages (15min) ──
                entry("creditsPage", defaultConfig, Duration.ofMinutes(15)),
                entry("peoplePage", defaultConfig, Duration.ofMinutes(15)),
                entry("episodesPage", defaultConfig, Duration.ofMinutes(15)),
                entry("seasonsPage", defaultConfig, Duration.ofMinutes(15)),
                entry("usersPage", defaultConfig, Duration.ofMinutes(15)),
                entry("watchlistPage", defaultConfig, Duration.ofMinutes(15)),
                entry("watchEntriesPage", defaultConfig, Duration.ofMinutes(15)),
                entry("watchlist", defaultConfig, Duration.ofMinutes(15)),
                entry("watchEntries", defaultConfig, Duration.ofMinutes(10)),

                // ── User data (5min) ──
                entry("userStats", defaultConfig, Duration.ofMinutes(5)),
                entry("userInsights", defaultConfig, Duration.ofMinutes(5)),
                entry("watchProgress", defaultConfig, Duration.ofMinutes(5)),

                // ── External API (6h) ──
                entry("tmdb", defaultConfig, Duration.ofHours(6))
        );

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private static Map.Entry<String, RedisCacheConfiguration> entry(
            String name, RedisCacheConfiguration base, Duration ttl) {
        return Map.entry(name, base.entryTtl(ttl));
    }
}
