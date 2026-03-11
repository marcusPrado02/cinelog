package com.cine.cinelog.core.application.services;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.strategy.RecommendationStrategy;
import com.cine.cinelog.core.domain.strategy.impl.CollaborativeRecommendationStrategy;
import com.cine.cinelog.core.domain.strategy.impl.ContentBasedRecommendationStrategy;
import com.cine.cinelog.core.domain.strategy.impl.HybridRecommendationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço para gerenciamento de recomendações de mídias.
 *
 * <p>
 * <strong>Feature:</strong> Recommendations (PR6 - Fase 6)
 *
 * <p>
 * Implementa Strategy Pattern para permitir diferentes algoritmos de recomendação:
 * <ul>
 *   <li><strong>Content-Based</strong>: Baseado em gêneros/atributos das mídias assistidas</li>
 *   <li><strong>Collaborative</strong>: Baseado em usuários com gostos similares</li>
 *   <li><strong>Hybrid</strong>: Combina múltiplas estratégias com pesos</li>
 * </ul>
 *
 * <p>
 * <strong>Cache:</strong> Recomendações são cacheadas por 15 minutos para reduzir overhead computacional.
 *
 * <p>
 * <strong>Fallback:</strong> Se uma estratégia falhar ou não tiver dados suficientes,
 * tenta estratégias alternativas automaticamente.
 *
 * @since 1.0 (PR6 - Fase 6)
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MIN_LIMIT = 1;

    private final ContentBasedRecommendationStrategy contentBasedStrategy;
    private final CollaborativeRecommendationStrategy collaborativeStrategy;
    private final HybridRecommendationStrategy hybridStrategy;

    public RecommendationService(
            ContentBasedRecommendationStrategy contentBasedStrategy,
            CollaborativeRecommendationStrategy collaborativeStrategy,
            HybridRecommendationStrategy hybridStrategy) {
        this.contentBasedStrategy = contentBasedStrategy;
        this.collaborativeStrategy = collaborativeStrategy;
        this.hybridStrategy = hybridStrategy;
    }

    /**
     * Gera recomendações usando a melhor estratégia disponível (Hybrid por padrão).
     *
     * <p>
     * Ordem de fallback:
     * 1. Hybrid (combina Content-Based + Collaborative)
     * 2. Content-Based (se houver histórico do usuário)
     * 3. Collaborative (se houver usuários similares)
     * 4. Lista vazia
     *
     * @param userId ID do usuário
     * @param limit máximo de recomendações (1-100, padrão: 20)
     * @return lista de mídias recomendadas
     */
    @Cacheable(value = "recommendations", key = "#userId + '_' + #limit")
    public List<Media> getRecommendations(Long userId, Integer limit) {
        int validatedLimit = validateLimit(limit);

        log.debug("Gerando recomendações para userId={}, limit={}", userId, validatedLimit);

        try {
            // Tentar Hybrid primeiro (melhor qualidade)
            if (hybridStrategy.isApplicable(userId)) {
                log.debug("Usando estratégia Hybrid para userId={}", userId);
                return hybridStrategy.recommend(userId, validatedLimit);
            }

            // Fallback para Content-Based
            if (contentBasedStrategy.isApplicable(userId)) {
                log.debug("Usando estratégia Content-Based para userId={}", userId);
                return contentBasedStrategy.recommend(userId, validatedLimit);
            }

            // Fallback para Collaborative
            if (collaborativeStrategy.isApplicable(userId)) {
                log.debug("Usando estratégia Collaborative para userId={}", userId);
                return collaborativeStrategy.recommend(userId, validatedLimit);
            }

            log.warn("Nenhuma estratégia disponível para userId={}. Retornando lista vazia.", userId);
            return List.of();

        } catch (Exception e) {
            log.error("Erro ao gerar recomendações para userId={}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Gera recomendações usando estratégia específica.
     *
     * @param userId ID do usuário
     * @param strategyName nome da estratégia (content-based, collaborative, hybrid)
     * @param limit máximo de recomendações
     * @return lista de mídias recomendadas
     * @throws IllegalArgumentException se estratégia não existir
     */
    @Cacheable(value = "recommendations", key = "#userId + '_' + #strategyName + '_' + #limit")
    public List<Media> getRecommendationsByStrategy(Long userId, String strategyName, Integer limit) {
        int validatedLimit = validateLimit(limit);

        String safeStrategyName = sanitizeForLogging(strategyName);

        log.debug("Gerando recomendações para userId={}, strategy={}, limit={}",
                userId, safeStrategyName, validatedLimit);

        RecommendationStrategy strategy = getStrategy(strategyName);

        if (!strategy.isApplicable(userId)) {
            log.warn("Estratégia {} não disponível para userId={}. Retornando lista vazia.",
                    safeStrategyName, userId);
            return List.of();
        }

        try {
            return strategy.recommend(userId, validatedLimit);
        } catch (Exception e) {
            log.error("Erro ao gerar recomendações com estratégia {} para userId={}: {}",
                    safeStrategyName, userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Verifica se há recomendações disponíveis para um usuário.
     *
     * @param userId ID do usuário
     * @return true se alguma estratégia está disponível
     */
    public boolean hasRecommendations(Long userId) {
        return hybridStrategy.isApplicable(userId)
                || contentBasedStrategy.isApplicable(userId)
                || collaborativeStrategy.isApplicable(userId);
    }

    /**
     * Lista estratégias disponíveis para um usuário.
     *
     * @param userId ID do usuário
     * @return lista de nomes de estratégias disponíveis
     */
    public List<String> getAvailableStrategies(Long userId) {
        List<String> available = new java.util.ArrayList<>();

        if (contentBasedStrategy.isApplicable(userId)) {
            available.add("content-based");
        }
        if (collaborativeStrategy.isApplicable(userId)) {
            available.add("collaborative");
        }
        if (hybridStrategy.isApplicable(userId)) {
            available.add("hybrid");
        }

        return available;
    }

    /**
     * Valida e ajusta limit.
     */
    private int validateLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < MIN_LIMIT) {
            log.warn("Limit {} abaixo do mínimo {}. Ajustando.", limit, MIN_LIMIT);
            return MIN_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            log.warn("Limit {} acima do máximo {}. Ajustando.", limit, MAX_LIMIT);
            return MAX_LIMIT;
        }
        return limit;
    }

    /**
     * Obtém estratégia por nome.
     */
    private RecommendationStrategy getStrategy(String strategyName) {
        return switch (strategyName.toLowerCase()) {
            case "content-based", "content_based" -> contentBasedStrategy;
            case "collaborative" -> collaborativeStrategy;
            case "hybrid" -> hybridStrategy;
            default -> throw new IllegalArgumentException(
                    "Estratégia desconhecida: " + strategyName +
                    ". Valores válidos: content-based, collaborative, hybrid");
        };
    }
    /**
     * Sanitiza valores para uso seguro em logs, removendo quebras de linha e caracteres de controle.
     *
     * @param input valor potencialmente fornecido pelo usuário
     * @return valor sanitizado, seguro para logging
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // remove CR, LF e outros caracteres de controle que podem quebrar o formato do log
            if (c >= 32 && c != 127) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
