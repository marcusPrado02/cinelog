package com.cine.cinelog.features.watchlist.web.dto;

/**
 * DTO de requisição para adicionar mídia à lista de desejos do usuário.
 * 
 * <p>
 * Lista de desejos (watchlist) é uma coleção de mídias que o usuário pretende
 * assistir futuramente:
 * <ul>
 * <li>mediaId: ID da mídia a ser adicionada à lista</li>
 * </ul>
 * 
 * @since 1.0
 */
public record WatchlistAddRequest(Long mediaId) {
}