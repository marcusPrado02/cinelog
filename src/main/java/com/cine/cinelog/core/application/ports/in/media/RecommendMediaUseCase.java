package com.cine.cinelog.core.application.ports.in.media;

import java.util.List;

import com.cine.cinelog.core.domain.model.MediaWithRating;

/**
 * Caso de uso para gerar recomendações personalizadas de mídias para um
 * usuário.
 * 
 * <p>
 * O algoritmo de recomendação considera:
 * <ul>
 * <li>Histórico de avaliações do usuário</li>
 * <li>Gêneros preferidos baseados em ratings altos</li>
 * <li>Popularidade geral das mídias (média de ratings)</li>
 * <li>Exclusão de mídias já assistidas</li>
 * </ul>
 * 
 * <p>
 * As recomendações são ordenadas por relevância, priorizando mídias populares
 * dos gêneros favoritos do usuário.
 * 
 * @since 1.0
 * @see MediaWithRating
 */
public interface RecommendMediaUseCase {

    /**
     * Gera uma lista de recomendações personalizadas para o usuário.
     * 
     * @param userId o identificador do usuário
     * @return lista de mídias recomendadas com suas avaliações médias
     */
    List<MediaWithRating> execute(Long userId);
}
