package com.cine.cinelog.features.users.web.dto;

import java.time.LocalDate;

/**
 * DTO de resposta contendo estatísticas agregadas do usuário.
 * 
 * <p>
 * Retorna métricas sobre as atividades do usuário na plataforma:
 * <ul>
 * <li>totalEntries: total de registros de visualização (watch entries) do
 * usuário</li>
 * <li>totalRated: total de mídias avaliadas pelo usuário</li>
 * <li>averageRating: média das avaliações atribuídas pelo usuário</li>
 * <li>firstWatchDate: data da primeira visualização registrada</li>
 * <li>lastWatchDate: data da última visualização registrada</li>
 * </ul>
 * 
 * <p>
 * Utilizado no endpoint de estatísticas do usuário para fornecer insights sobre
 * seu histórico de visualizações.
 * 
 * @since 1.0
 */
public record UserStatsResponse(
                long totalEntries,
                long totalRated,
                Double averageRating,
                LocalDate firstWatchDate,
                LocalDate lastWatchDate) {
}