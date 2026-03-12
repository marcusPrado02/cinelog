package com.cine.cinelog.core.application.usecase.watchentry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.cine.cinelog.core.domain.model.WatchEntryStatusType;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.watchentry.UpdateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryReferencePolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável por atualizar entradas de visualização existentes.
 *
 * <p>
 * Permite atualizar:
 * <ul>
 * <li>Data de visualização</li>
 * <li>Rating (avaliação de 0-10)</li>
 * <li>Comentário</li>
 * </ul>
 *
 * <p>
 * Aplica políticas específicas:
 * <ul>
 * <li>Validação de rating: usuário só pode avaliar após assistir</li>
 * <li>Validação de referências: garante que as entidades relacionadas
 * existem</li>
 * <li>Validação de atualização: garante integridade dos dados</li>
 * </ul>
 *
 * @since 1.0
 * @see UpdateWatchEntryUseCase
 * @see WatchEntryRepositoryPort
 * @see WatchEntryPolicy
 * @see RatingPolicy
 * @see WatchEntryReferencePolicy
 */
@Transactional
public class UpdateWatchEntryService implements UpdateWatchEntryUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateWatchEntryService.class);

    private final WatchEntryRepositoryPort repo;
    private final WatchEntryPolicy watchPolicy;
    private final RatingPolicy ratingPolicy;
    private final WatchEntryReferencePolicy referencePolicy;

    public UpdateWatchEntryService(WatchEntryRepositoryPort repo,
            WatchEntryPolicy watchPolicy,
            RatingPolicy ratingPolicy,
            WatchEntryReferencePolicy referencePolicy) {
        this.repo = repo;
        this.watchPolicy = watchPolicy;
        this.ratingPolicy = ratingPolicy;
        this.referencePolicy = referencePolicy;
    }

    /**
     * Executa a atualização de uma entrada de visualização existente.
     *
     * @param id                o identificador único da entrada a ser atualizada
     * @param entry             os novos dados da entrada
     * @param isRatingOperation indica se é uma operação de avaliação (rating)
     * @return a entrada atualizada e persistida
     * @throws DomainException com código {@link ErrorCode#INVALID_WATCH_ENTRY} se a
     *                         entrada não existir
     * @throws DomainException se as políticas de validação falharem
     */
    @Override
    @Observed(name = "watchentry.update", contextualName = "update-watchentry-service")
    @Measured("cinelog.service.watchentry.update")
    @AuditableAction(module = "WATCH_ENTRY", action = "UPDATE", description = "Atualização de registro de visualização")
    @Caching(evict = {
            @CacheEvict(value = "watchEntriesPage", allEntries = true),
            @CacheEvict(value = "watchEntryById", key = "#id")
    })
    public WatchEntry execute(Long id, WatchEntry entry, boolean isRatingOperation) {
        log.debug("Iniciando atualização de watch entry. Parâmetros: {}",
                Map.of("id", id, "isRatingOperation", isRatingOperation));

        try {
            log.debug("Buscando watch entry existente. ID: {}", id);
            var current = repo.findById(id).orElseThrow(
                    () -> DomainException.of(ErrorCode.INVALID_WATCH_ENTRY));

            log.debug("Watch entry encontrada. Aplicando atualizações");
            var updated = current.updateFrom(entry);

            // Se o entry mesclado ainda está em PLANNING mas já possui rating ou watchedAt,
            // auto-transiciona para COMPLETED (mesma lógica do CreateWatchEntryService)
            if ((updated.getRating() != null || updated.getWatchedAt() != null)
                    && updated.getStatusType() == WatchEntryStatusType.PLANNING) {
                LocalDate savedWatchedAt = updated.getWatchedAt();
                updated.startWatching(); // PLANNING → WATCHING
                updated.markAsCompleted(null); // WATCHING → COMPLETED
                if (savedWatchedAt != null)
                    updated.setWatchedAt(savedWatchedAt);
            }

            if (isRatingOperation && updated.getRating() != null) {
                log.debug("Validando rating. Rating: {}", updated.getRating());
                ratingPolicy.validateCanRate(updated, updated.getRating(), Instant.now());
            }

            log.debug("Validando políticas de atualização");
            watchPolicy.validateUpdate(updated);
            referencePolicy.validateUpdate(current, updated);

            updated.applyRating(updated.getRating(), updated.getComment());

            WatchEntry saved = repo.save(updated);
            log.info("Watch entry atualizada com sucesso. ID: {}", id);
            return saved;
        } catch (DomainException e) {
            log.warn("Erro de validação ao atualizar watch entry. ID: {}, Erro: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar watch entry. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
