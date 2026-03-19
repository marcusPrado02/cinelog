package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.events.DomainEventPublisherPort;
import com.cine.cinelog.core.application.ports.in.watchentry.CreateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryRatedEvent;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryUniquenessPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Serviço responsável por registrar uma nova entrada de visualização (watch
 * entry).
 *
 * <p>
 * Uma watch entry representa o histórico de visualização de um usuário,
 * incluindo:
 * <ul>
 * <li>Mídia ou episódio assistido</li>
 * <li>Data de visualização</li>
 * <li>Avaliação (rating) opcional</li>
 * <li>Comentário opcional</li>
 * </ul>
 *
 * <p>
 * Aplica políticas de domínio:
 * <ul>
 * <li>Validações de referências (usuário, mídia/episódio existem)</li>
 * <li>Validação de rating (0-10)</li>
 * <li>Unicidade: um usuário não pode ter múltiplas entradas para a mesma
 * mídia/episódio</li>
 * </ul>
 *
 * @since 1.0
 * @see CreateWatchEntryUseCase
 * @see WatchEntryRepositoryPort
 * @see WatchEntryPolicy
 * @see WatchEntryUniquenessPolicy
 */
public class CreateWatchEntryService implements CreateWatchEntryUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateWatchEntryService.class);

    private final WatchEntryRepositoryPort repo;
    private final WatchEntryPolicy policy;
    private final WatchEntryUniquenessPolicy uniquenessPolicy;
    private final DomainEventPublisherPort eventPublisher;

    public CreateWatchEntryService(WatchEntryRepositoryPort repo,
            WatchEntryPolicy policy,
            WatchEntryUniquenessPolicy uniquenessPolicy,
            DomainEventPublisherPort eventPublisher) {
        this.repo = repo;
        this.policy = policy;
        this.uniquenessPolicy = uniquenessPolicy;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executa o registro de uma nova entrada de visualização.
     *
     * @param entry a entrada de visualização a ser criada
     * @return a entrada criada e persistida
     * @throws DomainException se as validações de política falharem ou se já
     *                         existir
     *                         uma entrada para o mesmo usuário e mídia/episódio
     */
    @Override
    @Observed(name = "watchentry.create", contextualName = "create-watchentry-service")
    @Measured("cinelog.service.watchentry.create")
    @AuditableAction(module = "WATCH_ENTRY", action = "CREATE", description = "Registro de visualização")
    @CacheEvict(value = "watchEntriesPage", allEntries = true)
    public WatchEntry execute(WatchEntry entry) {
        log.debug("Iniciando criação de watch entry. Parâmetros: {}",
                Map.of("userId", String.valueOf(entry.getUserId()),
                        "mediaId", entry.getMediaId() != null ? entry.getMediaId() : "null",
                        "episodeId", entry.getEpisodeId() != null ? entry.getEpisodeId() : "null"));

        try {
            // Auto-transição de estado: se rating ou watchedAt informados, a entrada
            // representa uma visualização já concluída → transicionar PLANNING → COMPLETED
            // antes da validação de política (que usa setRating() state-aware).
            if (entry.getRating() != null || entry.getWatchedAt() != null) {
                LocalDate userWatchedAt = entry.getWatchedAt();
                entry.startWatching(); // PLANNING → WATCHING
                entry.markAsCompleted(null); // WATCHING → COMPLETED (watchedAt = today)
                if (userWatchedAt != null) {
                    entry.setWatchedAt(userWatchedAt); // preserva data fornecida pelo usuário
                }
                log.debug("Watch entry auto-transicionada para COMPLETED (rating/watchedAt presentes)");
            }

            // Regras de domínio (R1–R4)
            log.debug("Validando políticas de domínio para watch entry");
            policy.validateCreate(entry);

            // Regra de unicidade (R5)
            log.debug("Validando unicidade de watch entry");
            uniquenessPolicy.validate(entry);

            WatchEntry saved = repo.save(entry);
            log.info("Watch entry criada com sucesso. ID: {}", saved.getId());

            // Publicar evento de criação
            publishCreatedEvent(saved);

            // Se foi criado com rating, publicar evento de avaliação também
            if (saved.getRating() != null) {
                publishRatedEvent(saved, null);
            }

            return saved;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Erro de validação ao criar watch entry: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao criar watch entry. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void publishCreatedEvent(WatchEntry entry) {
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                entry.getId(),
                entry.getUserId(),
                entry.getMediaId(),
                entry.getEpisodeId(),
                entry.getWatchedAt(),
                entry.getRating());

        eventPublisher.publish(event);
        log.debug("Evento WatchEntryCreatedEvent publicado: {}", event.eventId());
    }

    private void publishRatedEvent(WatchEntry entry, BigDecimal previousRating) {
        WatchEntryRatedEvent event = WatchEntryRatedEvent.of(
                entry.getId(),
                entry.getUserId(),
                entry.getMediaId(),
                entry.getEpisodeId(),
                entry.getRating(),
                previousRating);

        eventPublisher.publish(event);
        log.debug("Evento WatchEntryRatedEvent publicado: {}", event.eventId());
    }
}
