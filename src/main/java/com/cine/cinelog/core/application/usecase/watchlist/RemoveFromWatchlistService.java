package com.cine.cinelog.core.application.usecase.watchlist;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.core.application.ports.in.watchlist.RemoveFromWatchlistUseCase;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
import com.cine.cinelog.core.domain.error.ForbiddenOperationException;
import com.cine.cinelog.core.domain.error.NotFoundException;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.security.AuthenticatedUser;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;

/**
 * Serviço responsável por remover mídias da watchlist do usuário autenticado.
 *
 * <p>
 * Remove o item da lista de desejos quando o usuário decide que não deseja mais
 * assistir ou já assistiu a mídia.
 *
 * <p>
 * Validações aplicadas:
 * <ul>
 * <li>Verifica se o item existe na watchlist do usuário</li>
 * <li>Garante que o usuário só pode remover itens da sua própria watchlist</li>
 * </ul>
 *
 * @since 1.0
 * @see RemoveFromWatchlistUseCase
 * @see WatchlistRepositoryPort
 * @see CurrentUserProvider
 */
public class RemoveFromWatchlistService implements RemoveFromWatchlistUseCase {
    private static final Logger log = LoggerFactory.getLogger(RemoveFromWatchlistService.class);

    private final WatchlistRepositoryPort repository;
    private final CurrentUserProvider currentUser;

    public RemoveFromWatchlistService(WatchlistRepositoryPort repository,
            CurrentUserProvider currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /**
     * Remove uma mídia da watchlist do usuário atual.
     *
     * @param mediaId o identificador da mídia a ser removida da watchlist
     * @throws NotFoundException           se o item não existir na watchlist do
     *                                     usuário
     * @throws ForbiddenOperationException se o usuário tentar remover item de outra
     *                                     watchlist
     */
    @Override
    @Observed(name = "watchlist.remove", contextualName = "remove-from-watchlist-service")
    @Measured("cinelog.service.watchlist.remove")
    @AuditableAction(module = "WATCHLIST", action = "REMOVE", description = "Remover mídia da watchlist")
    @CacheEvict(value = "watchlistPage", allEntries = true)
    public void remove(Long mediaId) {
        AuthenticatedUser user = currentUser.getRequiredCurrentUser();

        log.debug("Iniciando remoção da watchlist. Parâmetros: {}",
                Map.of("userId", user.id(), "mediaId", mediaId));

        try {
            log.debug("Buscando item da watchlist. UserId: {}, MediaId: {}", user.id(), mediaId);
            var existing = repository.findByUserIdAndMediaId(user.id(), mediaId)
                    .orElseThrow(() -> new NotFoundException("watchlist.not_found"));

            log.debug("Verificando propriedade do item");
            if (!existing.belongsTo(user.id())) {
                log.warn("Tentativa de remover item de outra watchlist. UserId: {}, ItemUserId: {}",
                        user.id(), existing.getUserId());
                throw new ForbiddenOperationException("not_allowed");
            }

            repository.deleteById(existing.getId());
            log.info("Mídia removida da watchlist com sucesso. ID: {}, UserId: {}, MediaId: {}",
                    existing.getId(), user.id(), mediaId);
        } catch (NotFoundException | ForbiddenOperationException e) {
            log.warn("Erro ao remover da watchlist. UserId: {}, MediaId: {}, Erro: {}",
                    user.id(), mediaId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao remover da watchlist. UserId: {}, MediaId: {}, Erro: {}",
                    user.id(), mediaId, e.getMessage(), e);
            throw e;
        }
    }
}
