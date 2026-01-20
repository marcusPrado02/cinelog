package com.cine.cinelog.features.watchlist.web.dto;

/**
 * DTO de resposta contendo informações de um item da lista de desejos.
 * 
 * <p>
 * Retorna os dados do item na lista de desejos do usuário:
 * <ul>
 * <li>id: identificador único do item na watchlist</li>
 * <li>mediaId: ID da mídia na lista</li>
 * <li>addedAt: data/hora em que foi adicionado à lista</li>
 * </ul>
 * 
 * @since 1.0
 */
public record WatchlistResponse(Long id, Long mediaId, String addedAt) {
}