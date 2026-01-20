package com.cine.cinelog.core.application.usecase.watchlist;

import java.util.Map;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.core.application.ports.in.watchlist.ListMyWatchlistUseCase;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.security.AuthenticatedUser;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

/**
 * Serviço responsável por listar todos os itens da watchlist do usuário
 * autenticado.
 * 
 * <p>
 * Retorna uma lista paginada de todas as mídias que o usuário marcou como
 * desejadas
 * para assistir no futuro.
 * 
 * @since 1.0
 * @see ListMyWatchlistUseCase
 * @see WatchlistRepositoryPort
 * @see CurrentUserProvider
 */
public class ListMyWatchlistService implements ListMyWatchlistUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListMyWatchlistService.class);

    private final WatchlistRepositoryPort repository;
    private final CurrentUserProvider currentUser;

    public ListMyWatchlistService(WatchlistRepositoryPort repository,
            CurrentUserProvider currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /**
     * Lista todos os itens da watchlist do usuário atual de forma paginada.
     * 
     * @param pageQuery os parâmetros de paginação
     * @return resultado paginado contendo os itens da watchlist do usuário
     */
    @Override
    @Observed(name = "watchlist.list", contextualName = "list-my-watchlist-service")
    @Cacheable(value = "watchlistPage", key = "#pageQuery.toString()")
    @Measured("cinelog.service.watchlist.list")
    @AlertIfSlow(thresholdMs = 800)
    public PageResult<WatchlistItem> execute(PageQuery pageQuery) {
        AuthenticatedUser user = currentUser.getRequiredCurrentUser();

        log.debug("Iniciando listagem de watchlist. Parâmetros: {}",
                Map.of("userId", user.id(), "page", pageQuery.page(), "size", pageQuery.size()));

        try {
            PageResult<WatchlistItem> result = repository.findAllByUserId(user.id());
            log.debug("Listagem de watchlist concluída. UserId: {}, Total encontrado: {}",
                    user.id(), result.totalElements());
            return result;
        } catch (Exception e) {
            log.error("Erro inesperado ao listar watchlist. UserId: {}, Erro: {}",
                    user.id(), e.getMessage(), e);
            throw e;
        }
    }
}