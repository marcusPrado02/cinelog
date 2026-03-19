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
     * Configura um CacheManager baseado em Redis com TTLs diferentes por cache.
     *
     * <p>
     * <b>Rationale dos TTLs:</b>
     * <ul>
     * <li>{@code genres} — 24h: listagem de gêneros muda raramente (dados de
     * catálogo)</li>
     * <li>{@code media} — 2h: fichas de mídias são estáveis mas podem ser
     * atualizadas pelo TMDB sync</li>
     * <li>{@code watchlist} — 15min: usuário pode adicionar/remover itens a
     * qualquer momento</li>
     * <li>{@code watchEntries} — 10min: progresso atualiza frequentemente durante
     * sessões de streaming</li>
     * <li>{@code userStats} — 5min: estatísticas refletem ações recentes do
     * usuário</li>
     * <li>{@code recommendations} — 1h: algoritmo de recomendação é pesado; cache
     * reduz recálculo</li>
     * <li>default — 6h: fallback conservador para caches não configurados
     * explicitamente</li>
     * </ul>
     * </p>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "genres", defaultConfig.entryTtl(Duration.ofHours(24)),
                "media", defaultConfig.entryTtl(Duration.ofHours(2)),
                "watchlist", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                "watchEntries", defaultConfig.entryTtl(Duration.ofMinutes(10)),
                "userStats", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "recommendations", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
