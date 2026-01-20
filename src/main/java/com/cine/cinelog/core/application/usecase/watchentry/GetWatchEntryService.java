package com.cine.cinelog.core.application.usecase.watchentry;

import java.util.Map;

import org.springframework.cache.annotation.Cacheable;

import com.cine.cinelog.core.application.ports.in.watchentry.GetWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável por buscar uma entrada de visualização específica por ID.
 * 
 * <p>
 * Recupera os dados completos de uma visualização, incluindo usuário,
 * mídia/episódio,
 * data, rating e comentário.
 * 
 * @since 1.0
 * @see GetWatchEntryUseCase
 * @see WatchEntryRepositoryPort
 */
public class GetWatchEntryService implements GetWatchEntryUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetWatchEntryService.class);

    private final WatchEntryRepositoryPort repo;

    public GetWatchEntryService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca uma entrada de visualização por seu identificador único.
     * 
     * @param id o identificador único da entrada de visualização
     * @return a entrada de visualização encontrada
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se a
     *                         entrada não existir
     */
    @Override
    @Observed(name = "watchentry.get", contextualName = "get-watchentry-service")
    @Measured("cinelog.service.watchentry.get")
    @AlertIfSlow(thresholdMs = 500)
    @Cacheable(value = "watchEntryById", key = "#id")
    public WatchEntry execute(Long id) {
        log.debug("Buscando watch entry. ID: {}", id);
        try {
            WatchEntry entry = repo.findById(id).orElseThrow(() -> DomainException.of(
                    ErrorCode.GEN_NOT_FOUND, "WatchEntry not found: " + id));
            log.debug("Watch entry encontrada. ID: {}, UserId: {}", id, entry.getUserId());
            return entry;
        } catch (DomainException e) {
            log.warn("Watch entry não encontrada. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar watch entry. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
