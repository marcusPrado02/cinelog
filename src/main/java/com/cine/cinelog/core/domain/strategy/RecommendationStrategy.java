package com.cine.cinelog.core.domain.strategy;

import com.cine.cinelog.core.domain.model.Media;

import java.util.List;

/**
 * Strategy Pattern - Interface para diferentes algoritmos de recomendação.
 *
 * <p>
 * Permite trocar dinamicamente o algoritmo de recomendação sem alterar
 * o código cliente. Implementações possíveis:
 * <ul>
 * <li>Content-Based: recomenda mídias similares baseado em
 * gêneros/atributos</li>
 * <li>Collaborative Filtering: recomenda baseado em usuários com gostos
 * similares</li>
 * <li>Hybrid: combina múltiplas estratégias</li>
 * <li>Trending: recomenda mídias populares/em alta</li>
 * </ul>
 *
 * <p>
 * <strong>Benefícios do Pattern</strong>:
 * <ul>
 * <li>Open/Closed Principle: novas estratégias podem ser adicionadas sem
 * modificar código existente</li>
 * <li>Single Responsibility: cada estratégia tem uma responsabilidade
 * específica</li>
 * <li>Testabilidade: cada algoritmo pode ser testado isoladamente</li>
 * <li>Flexibilidade: estratégia pode ser trocada em runtime</li>
 * </ul>
 *
 * @since 1.0
 * @see ContentBasedRecommendationStrategy
 * @see CollaborativeRecommendationStrategy
 * @see HybridRecommendationStrategy
 */
public interface RecommendationStrategy {

    /**
     * Gera recomendações de mídias para um usuário.
     *
     * @param userId o identificador do usuário
     * @param limit  quantidade máxima de recomendações
     * @return lista de mídias recomendadas, ordenadas por relevância
     */
    List<Media> recommend(Long userId, int limit);

    /**
     * Retorna o nome da estratégia para fins de logging/métricas.
     *
     * @return nome da estratégia (ex: "content-based", "collaborative", "hybrid")
     */
    String getStrategyName();

    /**
     * Indica se esta estratégia está disponível para uso.
     * Pode retornar false se não há dados suficientes.
     *
     * @param userId o identificador do usuário
     * @return true se a estratégia pode ser aplicada
     */
    default boolean isApplicable(Long userId) {
        return true;
    }
}
