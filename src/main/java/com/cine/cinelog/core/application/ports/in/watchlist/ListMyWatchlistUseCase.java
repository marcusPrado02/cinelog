package com.cine.cinelog.core.application.ports.in.watchlist;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import java.util.List;

/**
 * Caso de uso para listar todos os itens da watchlist do usuário autenticado.
 * 
 * <p>
 * Retorna uma lista paginada de todas as mídias que o usuário marcou como
 * desejadas para assistir no futuro.
 * 
 * @since 1.0
 * @see WatchlistItem
 * @see PageQuery
 */
public interface ListMyWatchlistUseCase {
    /**
     * Lista todos os itens da watchlist do usuário autenticado.
     * 
     * @param pageQuery parâmetros de paginação
     * @return resultado paginado com os itens da watchlist
     */
    PageResult<WatchlistItem> execute(PageQuery pageQuery);
}
