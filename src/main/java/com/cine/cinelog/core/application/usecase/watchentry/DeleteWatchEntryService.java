package com.cine.cinelog.core.application.usecase.watchentry;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.watchentry.DeleteWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

/**
 * Serviço responsável por excluir entradas de visualização do sistema.
 *
 * <p>
 * Remove o registro de uma visualização, incluindo rating e comentário
 * associados.
 *
 * @since 1.0
 * @see DeleteWatchEntryUseCase
 * @see WatchEntryRepositoryPort
 */
public class DeleteWatchEntryService implements DeleteWatchEntryUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteWatchEntryService.class);

    private final WatchEntryRepositoryPort repo;

    public DeleteWatchEntryService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a exclusão de uma entrada de visualização.
     *
     * @param id o identificador único da entrada a ser excluída
     */
    @Override
    @Observed(name = "watchentry.delete", contextualName = "delete-watchentry-service")
    @Measured("cinelog.service.watchentry.delete")
    @AuditableAction(module = "WATCH_ENTRY", action = "DELETE", description = "Exclusão de registro de visualização")
    @Caching(evict = {
            @CacheEvict(value = "watchEntriesPage", allEntries = true),
            @CacheEvict(value = "watchEntryById", key = "#id")
    })
    public void execute(Long id) {
        log.debug("Iniciando exclusão de watch entry. ID: {}", id);
        try {
            repo.deleteById(id);
            log.info("Watch entry excluída com sucesso. ID: {}", id);
        } catch (Exception e) {
            log.error("Erro ao excluir watch entry. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
