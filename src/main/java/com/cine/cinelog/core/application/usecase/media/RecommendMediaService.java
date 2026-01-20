package com.cine.cinelog.core.application.usecase.media;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.RecommendMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.MediaWithRating;
import com.cine.cinelog.core.domain.spec.IsPopularMediaSpecification;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável por recomendar mídias personalizadas para um usuário.
 * 
 * <p>
 * Este caso de uso implementa um sistema de recomendação que sugere mídias
 * (filmes e séries) baseado em critérios de popularidade e relevância para o
 * usuário.
 * 
 * <p>
 * Algoritmo de recomendação:
 * <ol>
 * <li>Busca mídias candidatas para o usuário (ainda não assistidas)</li>
 * <li>Aplica especificação de popularidade
 * ({@link IsPopularMediaSpecification})</li>
 * <li>Filtra apenas mídias com avaliação média acima do limiar (padrão:
 * 6.0)</li>
 * <li>Retorna lista ordenada de recomendações</li>
 * </ol>
 * 
 * <p>
 * As recomendações consideram:
 * <ul>
 * <li>Mídias que o usuário ainda não assistiu</li>
 * <li>Avaliação média de outros usuários</li>
 * <li>Popularidade geral da mídia</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link RecommendMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}.
 * 
 * @since 1.0
 * @see RecommendMediaUseCase
 * @see IsPopularMediaSpecification
 * @see MediaWithRating
 */
@Transactional(readOnly = true)
public class RecommendMediaService implements RecommendMediaUseCase {
    private static final Logger log = LoggerFactory.getLogger(RecommendMediaService.class);

    private final MediaRepositoryPort mediaRepositoryPort;
    private final IsPopularMediaSpecification popularSpec;

    public RecommendMediaService(MediaRepositoryPort mediaRepositoryPort) {
        this.mediaRepositoryPort = mediaRepositoryPort;
        // limiar pode vir de configuração, aqui fixo um default
        this.popularSpec = new IsPopularMediaSpecification(BigDecimal.valueOf(6.0));
    }

    /**
     * Gera recomendações personalizadas de mídias para um usuário específico.
     * 
     * <p>
     * O algoritmo busca mídias que o usuário ainda não assistiu e aplica
     * filtros de qualidade baseados na avaliação média de outros usuários.
     * 
     * @param userId o identificador do usuário para quem gerar recomendações
     * @return lista de mídias recomendadas com suas avaliações médias
     */
    @Override
    @Observed(name = "media.recommend", contextualName = "recommend-media-service")
    @Measured("cinelog.service.media.recommend")
    @AlertIfSlow(thresholdMs = 1500, metricName = "cinelog.slow_recommendation_algorithm")
    public List<MediaWithRating> execute(Long userId) {
        log.debug("Iniciando geração de recomendações. UserId: {}", userId);

        try {
            log.debug("Buscando mídias candidatas para recomendação");
            List<MediaWithRating> candidates = mediaRepositoryPort.findCandidatesForUser(userId);

            log.debug("Candidatos encontrados: {}", candidates.size());

            // ST3: só deixa passar quem satisfaz o minAverage
            List<MediaWithRating> recommendations = candidates.stream()
                    .filter(popularSpec::isSatisfiedBy)
                    .toList();

            log.info("Recomendações geradas com sucesso. UserId: {}, Total recomendações: {}, Total candidatos: {}",
                    userId, recommendations.size(), candidates.size());

            return recommendations;
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar recomendações. UserId: {}, Erro: {}",
                    userId, e.getMessage(), e);
            throw e;
        }
    }
}
