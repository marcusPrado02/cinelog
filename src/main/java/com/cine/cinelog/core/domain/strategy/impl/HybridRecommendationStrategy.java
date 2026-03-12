package com.cine.cinelog.core.domain.strategy.impl;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.strategy.RecommendationStrategy;
import com.cine.cinelog.shared.observability.aop.Measured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Estratégia de recomendação híbrida (Hybrid Recommendation).
 *
 * <p>
 * Combina múltiplas estratégias para obter os benefícios de cada uma
 * e mitigar suas desvantagens individuais.
 *
 * <p>
 * <strong>Algoritmo</strong>:
 * <ol>
 * <li>Tenta aplicar estratégia primária (ex: content-based)</li>
 * <li>Se primária não aplicável ou retorna poucos resultados, usa secundária
 * (ex: collaborative)</li>
 * <li>Combina resultados das duas estratégias (intercalado ou weighted)</li>
 * <li>Remove duplicatas mantendo ordem de relevância</li>
 * <li>Retorna top N recomendações</li>
 * </ol>
 *
 * <p>
 * <strong>Vantagens</strong>:
 * <ul>
 * <li>Melhor cobertura: sempre tem alguma estratégia aplicável</li>
 * <li>Diversidade: combina diferentes fontes de recomendação</li>
 * <li>Robustez: se uma estratégia falha, tem fallback</li>
 * <li>Qualidade: combina precision (content) + serendipity (collaborative)</li>
 * </ul>
 *
 * <p>
 * <strong>Implementação Atual</strong>:
 * Weighted average (70% content-based, 30% collaborative).
 *
 * @since 1.0
 */
@Component
public class HybridRecommendationStrategy implements RecommendationStrategy {

    private static final Logger log = LoggerFactory.getLogger(HybridRecommendationStrategy.class);
    private static final double CONTENT_WEIGHT = 0.7;
    private static final double COLLABORATIVE_WEIGHT = 0.3;

    private final RecommendationStrategy contentBasedStrategy;
    private final RecommendationStrategy collaborativeStrategy;

    public HybridRecommendationStrategy(
            RecommendationStrategy contentBasedStrategy,
            RecommendationStrategy collaborativeStrategy) {
        this.contentBasedStrategy = contentBasedStrategy;
        this.collaborativeStrategy = collaborativeStrategy;
    }

    @Override
    @Measured("cinelog.strategy.recommendation.hybrid")
    public List<Media> recommend(Long userId, int limit) {
        log.debug("Iniciando recomendação hybrid. userId={}, limit={}", userId, limit);

        try {
            Set<Media> combinedRecommendations = new LinkedHashSet<>();

            // Calcular quantos itens pegar de cada estratégia
            int contentLimit = (int) Math.ceil(limit * CONTENT_WEIGHT);
            int collaborativeLimit = (int) Math.ceil(limit * COLLABORATIVE_WEIGHT);

            // 1. Tentar content-based primeiro
            if (contentBasedStrategy.isApplicable(userId)) {
                var contentRecs = contentBasedStrategy.recommend(userId, contentLimit);
                log.debug("Content-based retornou {} recomendações", contentRecs.size());
                combinedRecommendations.addAll(contentRecs);
            }

            // 2. Adicionar collaborative para diversificar
            if (collaborativeStrategy.isApplicable(userId)) {
                var collaborativeRecs = collaborativeStrategy.recommend(userId, collaborativeLimit);
                log.debug("Collaborative retornou {} recomendações", collaborativeRecs.size());
                combinedRecommendations.addAll(collaborativeRecs);
            }

            // 3. Converter para lista e limitar
            List<Media> result = new ArrayList<>(combinedRecommendations)
                    .stream()
                    .limit(limit)
                    .toList();

            log.info("Recomendação hybrid concluída. userId={}, recomendações={} (content+collaborative)",
                    userId, result.size());

            return result;

        } catch (Exception e) {
            log.error("Erro ao gerar recomendações hybrid. userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public String getStrategyName() {
        return "hybrid";
    }

    @Override
    public boolean isApplicable(Long userId) {
        // Hybrid sempre aplicável se pelo menos uma estratégia for aplicável
        boolean contentApplicable = contentBasedStrategy.isApplicable(userId);
        boolean collaborativeApplicable = collaborativeStrategy.isApplicable(userId);
        boolean applicable = contentApplicable || collaborativeApplicable;

        log.debug("Hybrid applicable para userId={}? {} (content={}, collaborative={})",
                userId, applicable, contentApplicable, collaborativeApplicable);

        return applicable;
    }
}
