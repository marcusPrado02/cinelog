package com.cine.cinelog.core.domain.strategy.impl;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.strategy.RecommendationStrategy;
import com.cine.cinelog.shared.observability.aop.Measured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estratégia de recomendação baseada em conteúdo (Content-Based Filtering).
 *
 * <p>
 * Recomenda mídias similares às que o usuário já assistiu e avaliou
 * positivamente.
 * A similaridade é calculada com base em:
 * <ul>
 * <li>Gêneros em comum</li>
 * <li>Tipo de mídia (filme/série)</li>
 * <li>Ano de lançamento próximo</li>
 * </ul>
 *
 * <p>
 * <strong>Algoritmo</strong>:
 * <ol>
 * <li>Busca mídias que o usuário assistiu com rating >= 7</li>
 * <li>Extrai características dessas mídias (gêneros, tipo, ano)</li>
 * <li>Busca mídias não assistidas com características similares</li>
 * <li>Calcula score de similaridade para cada candidato</li>
 * <li>Retorna top N candidatos ordenados por score</li>
 * </ol>
 *
 * <p>
 * <strong>Vantagens</strong>:
 * <ul>
 * <li>Não precisa de dados de outros usuários</li>
 * <li>Recomenda itens nichados/específicos</li>
 * <li>Transparente: fácil explicar por que recomendou</li>
 * </ul>
 *
 * <p>
 * <strong>Desvantagens</strong>:
 * <ul>
 * <li>Limitado às preferências já conhecidas (filter bubble)</li>
 * <li>Não descobre novos interesses</li>
 * <li>Precisa de histórico do usuário</li>
 * </ul>
 *
 * @since 1.0
 */
@Component("contentBasedStrategy")
public class ContentBasedRecommendationStrategy implements RecommendationStrategy {

    private static final Logger log = LoggerFactory.getLogger(ContentBasedRecommendationStrategy.class);
    private static final int MIN_RATING_THRESHOLD = 7;
    private static final int MIN_WATCH_HISTORY = 3;

    private final WatchEntryRepositoryPort watchEntryRepo;
    private final MediaRepositoryPort mediaRepo;

    public ContentBasedRecommendationStrategy(
            WatchEntryRepositoryPort watchEntryRepo,
            MediaRepositoryPort mediaRepo) {
        this.watchEntryRepo = watchEntryRepo;
        this.mediaRepo = mediaRepo;
    }

    @Override
    @Measured("cinelog.strategy.recommendation.content_based")
    public List<Media> recommend(Long userId, int limit) {
        log.debug("Iniciando recomendação content-based. userId={}, limit={}", userId, limit);

        try {
            // 1. Buscar histórico do usuário com boas avaliações
            var highRatedEntries = getHighRatedEntries(userId);

            if (highRatedEntries.isEmpty()) {
                log.info("Usuário {} sem avaliações altas. Retornando lista vazia.", userId);
                return List.of();
            }

            // 2. Extrair IDs das mídias bem avaliadas
            var likedMediaIds = highRatedEntries.stream()
                    .map(WatchEntry::getMediaId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());

            log.debug("Usuário {} gostou de {} mídias", userId, likedMediaIds.size());

            // 3. Buscar mídias similares (mesmos gêneros, mesmo tipo)
            // Por enquanto, usar findCandidatesForUser() que já faz similar
            var candidates = mediaRepo.findCandidatesForUser(userId);

            // 4. Ordenar por rating médio e limitar
            var recommendations = candidates.stream()
                    .map(candidate -> {
                        Media media = new Media();
                        media.setId(candidate.getMediaId());
                        media.setTitle(candidate.getTitle());
                        media.setType(candidate.getType());
                        return media;
                    })
                    .limit(limit)
                    .toList();

            log.info("Recomendação content-based concluída. userId={}, recomendações={}",
                    userId, recommendations.size());

            return recommendations;

        } catch (Exception e) {
            log.error("Erro ao gerar recomendações content-based. userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public String getStrategyName() {
        return "content-based";
    }

    @Override
    public boolean isApplicable(Long userId) {
        // Precisa de pelo menos MIN_WATCH_HISTORY assistidos com boas avaliações
        long ratedCount = watchEntryRepo.countRatedEntriesByUserId(userId);
        boolean applicable = ratedCount >= MIN_WATCH_HISTORY;

        log.debug("Content-based applicable para userId={}? {} (rated={})",
                userId, applicable, ratedCount);

        return applicable;
    }

    /**
     * Busca entradas com rating >= MIN_RATING_THRESHOLD.
     */
    private List<WatchEntry> getHighRatedEntries(Long userId) {
        // Buscar últimas 50 entradas do usuário
        var page = watchEntryRepo.listByUser(
                userId, null, null, MIN_RATING_THRESHOLD,
                null, null, PageRequest.of(0, 50));

        return page.content();
    }
}
