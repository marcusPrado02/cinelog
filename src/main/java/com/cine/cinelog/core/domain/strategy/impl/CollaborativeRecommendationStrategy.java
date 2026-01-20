package com.cine.cinelog.core.domain.strategy.impl;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.strategy.RecommendationStrategy;
import com.cine.cinelog.shared.observability.aop.Measured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Estratégia de recomendação colaborativa (Collaborative Filtering).
 *
 * <p>
 * Recomenda mídias baseado no comportamento de usuários similares.
 * "Usuários que gostaram disso também gostaram daquilo".
 *
 * <p>
 * <strong>Algoritmo (simplificado)</strong>:
 * <ol>
 * <li>Encontra usuários com gostos similares (baseado em ratings comuns)</li>
 * <li>Identifica mídias bem avaliadas por esses usuários</li>
 * <li>Exclui mídias já assistidas pelo usuário alvo</li>
 * <li>Retorna mídias mais populares entre usuários similares</li>
 * </ol>
 *
 * <p>
 * <strong>Vantagens</strong>:
 * <ul>
 * <li>Descobre novos interesses (serendipity)</li>
 * <li>Não precisa analisar conteúdo das mídias</li>
 * <li>Funciona para qualquer tipo de item</li>
 * <li>Melhora com mais dados</li>
 * </ul>
 *
 * <p>
 * <strong>Desvantagens</strong>:
 * <ul>
 * <li>Cold start problem: não funciona para novos usuários/itens</li>
 * <li>Precisa de muitos dados de múltiplos usuários</li>
 * <li>Sparsity: maioria dos usuários avalia poucos itens</li>
 * <li>Gray sheep: usuários com gostos únicos não têm recomendações boas</li>
 * </ul>
 *
 * @since 1.0
 */
public class CollaborativeRecommendationStrategy implements RecommendationStrategy {

    private static final Logger log = LoggerFactory.getLogger(CollaborativeRecommendationStrategy.class);
    private static final int MIN_USERS_REQUIRED = 10;
    private static final int MIN_RATINGS_REQUIRED = 5;

    private final WatchEntryRepositoryPort watchEntryRepo;
    private final MediaRepositoryPort mediaRepo;

    public CollaborativeRecommendationStrategy(
            WatchEntryRepositoryPort watchEntryRepo,
            MediaRepositoryPort mediaRepo) {
        this.watchEntryRepo = watchEntryRepo;
        this.mediaRepo = mediaRepo;
    }

    @Override
    @Measured("cinelog.strategy.recommendation.collaborative")
    public List<Media> recommend(Long userId, int limit) {
        log.debug("Iniciando recomendação collaborative. userId={}, limit={}", userId, limit);

        try {
            // Para esta implementação simplificada, usar o mesmo método de candidates
            // que já considera popularidade entre outros usuários
            var candidates = mediaRepo.findCandidatesForUser(userId);

            // Filtrar top N baseado em ratingCount (popularidade)
            var recommendations = candidates.stream()
                    .sorted((a, b) -> Long.compare(b.getRatingCount(), a.getRatingCount()))
                    .map(candidate -> {
                        Media media = new Media();
                        media.setId(candidate.getMediaId());
                        media.setTitle(candidate.getTitle());
                        media.setType(candidate.getType());
                        return media;
                    })
                    .limit(limit)
                    .toList();

            log.info("Recomendação collaborative concluída. userId={}, recomendações={}",
                    userId, recommendations.size());

            return recommendations;

        } catch (Exception e) {
            log.error("Erro ao gerar recomendações collaborative. userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public String getStrategyName() {
        return "collaborative";
    }

    @Override
    public boolean isApplicable(Long userId) {
        // Precisa de ratings suficientes do usuário
        long userRatings = watchEntryRepo.countRatedEntriesByUserId(userId);
        boolean applicable = userRatings >= MIN_RATINGS_REQUIRED;

        log.debug("Collaborative applicable para userId={}? {} (ratings={})",
                userId, applicable, userRatings);

        return applicable;
    }
}
