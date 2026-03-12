package com.cine.cinelog.core.application.usecase.watchlist;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.core.application.ports.in.watchlist.AddToWatchlistUseCase;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
import com.cine.cinelog.core.domain.error.DuplicateException;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.core.domain.policy.WatchlistReferencePolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.security.AuthenticatedUser;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;

/**
 * Serviço responsável por adicionar mídias à watchlist (lista de desejo) do
 * usuário autenticado.
 *
 * <p>
 * A watchlist permite aos usuários marcarem filmes e séries que desejam
 * assistir no futuro.
 * Cada item é vinculado ao usuário que o adicionou e à mídia desejada.
 *
 * <p>
 * Validações aplicadas:
 * <ul>
 * <li>Verifica se a mídia existe através da
 * {@link WatchlistReferencePolicy}</li>
 * <li>Impede duplicatas: um usuário não pode adicionar a mesma mídia duas
 * vezes</li>
 * <li>Registra automaticamente a data de adição</li>
 * </ul>
 *
 * @since 1.0
 * @see AddToWatchlistUseCase
 * @see WatchlistRepositoryPort
 * @see WatchlistReferencePolicy
 * @see CurrentUserProvider
 */
public class AddToWatchlistService implements AddToWatchlistUseCase {
    private static final Logger log = LoggerFactory.getLogger(AddToWatchlistService.class);

    private final WatchlistRepositoryPort repository;
    private final CurrentUserProvider currentUser;
    private final WatchlistReferencePolicy referencePolicy;

    public AddToWatchlistService(WatchlistRepositoryPort repository,
            CurrentUserProvider currentUser, WatchlistReferencePolicy referencePolicy) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.referencePolicy = referencePolicy;
    }

    /**
     * Adiciona uma mídia à watchlist do usuário atual.
     *
     * @param command comando contendo o ID da mídia a ser adicionada
     * @return o item da watchlist criado e persistido
     * @throws DuplicateException se o usuário já tiver essa mídia na watchlist
     * @throws DomainException    se a mídia não existir (via
     *                            WatchlistReferencePolicy)
     */
    @Override
    @Observed(name = "watchlist.add", contextualName = "add-to-watchlist-service")
    @Measured("cinelog.service.watchlist.add")
    @AuditableAction(module = "WATCHLIST", action = "ADD", description = "Adicionar mídia à watchlist")
    @CacheEvict(value = "watchlistPage", allEntries = true)
    public WatchlistItem add(AddCommand command) {
        AuthenticatedUser user = currentUser.getRequiredCurrentUser();

        log.debug("Iniciando adição à watchlist. Parâmetros: {}",
                Map.of("userId", user.id(), "mediaId", command.mediaId()));

        try {
            log.debug("Verificando duplicata de watchlist item");
            repository.findByUserIdAndMediaId(user.id(), command.mediaId())
                    .ifPresent(existing -> {
                        throw new DuplicateException("watchlist.item_already_exists");
                    });

            WatchlistItem item = new WatchlistItem(
                    null,
                    user.id(),
                    command.mediaId(),
                    LocalDateTime.now(ZoneOffset.UTC));

            log.debug("Validando referências da watchlist");
            referencePolicy.validateCreate(item);

            WatchlistItem saved = repository.save(item);
            log.info("Mídia adicionada à watchlist com sucesso. ID: {}, UserId: {}, MediaId: {}",
                    saved.getId(), user.id(), command.mediaId());
            return saved;
        } catch (DuplicateException e) {
            log.warn("Tentativa de adicionar mídia duplicada à watchlist. UserId: {}, MediaId: {}",
                    user.id(), command.mediaId());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao adicionar à watchlist. UserId: {}, MediaId: {}, Erro: {}",
                    user.id(), command.mediaId(), e.getMessage(), e);
            throw e;
        }
    }
}
