package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.UserStats;

/**
 * Caso de uso para obter estatísticas de visualização do usuário autenticado.
 * 
 * <p>
 * Retorna um resumo das atividades do usuário no sistema, incluindo:
 * <ul>
 * <li>Total de filmes assistidos</li>
 * <li>Total de episódios assistidos</li>
 * <li>Rating médio atribuído</li>
 * <li>Data do primeiro registro</li>
 * <li>Data do último registro</li>
 * </ul>
 * 
 * @since 1.0
 * @see UserStats
 */
public interface GetMyStatsUseCase {
    /**
     * Recupera as estatísticas do usuário autenticado.
     * 
     * @return objeto contendo as estatísticas calculadas
     */
    UserStats execute();
}