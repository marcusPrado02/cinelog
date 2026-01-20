package com.cine.cinelog.core.application.ports.in.watchlist;

/**
 * Caso de uso para remover uma mídia da watchlist (lista de desejos) do
 * usuário.
 * 
 * <p>
 * Remove o item da lista quando o usuário decide que não deseja mais assistir
 * ou já assistiu a mídia. O sistema valida que:
 * <ul>
 * <li>O item existe na watchlist do usuário</li>
 * <li>Apenas o proprietário pode remover items da sua própria watchlist</li>
 * </ul>
 * 
 * @since 1.0
 */
public interface RemoveFromWatchlistUseCase {
    /**
     * Remove uma mídia da watchlist do usuário autenticado.
     * 
     * @param mediaId o identificador da mídia a ser removida
     */
    void remove(Long mediaId);
}