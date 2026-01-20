package com.cine.cinelog.core.application.ports.in.watchlist;

import com.cine.cinelog.core.domain.model.WatchlistItem;

/**
 * Caso de uso para adicionar uma mídia à watchlist (lista de desejos) do
 * usuário.
 * 
 * <p>
 * Permite que o usuário marque filmes e séries que deseja assistir no futuro.
 * O sistema garante que:
 * <ul>
 * <li>A mídia existe no sistema</li>
 * <li>Não há duplicatas (um usuário não pode adicionar a mesma mídia duas
 * vezes)</li>
 * <li>A data de adição é registrada automaticamente</li>
 * </ul>
 * 
 * @since 1.0
 * @see WatchlistItem
 */
public interface AddToWatchlistUseCase {

    /**
     * Adiciona uma mídia à watchlist do usuário autenticado.
     * 
     * @param command comando contendo o ID da mídia a ser adicionada
     * @return o item da watchlist criado
     */
    WatchlistItem add(AddCommand command);

    /**
     * Comando para adicionar uma mídia à watchlist.
     * 
     * @param mediaId o identificador da mídia a ser adicionada
     */
    record AddCommand(Long mediaId) {
    }
}