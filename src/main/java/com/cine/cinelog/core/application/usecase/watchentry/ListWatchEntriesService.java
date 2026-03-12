package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.watchentry.ListWatchEntriesUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por listar entradas de visualização com filtros e
 * paginação.
 *
 * <p>
 * Permite listar o histórico de visualizações de um usuário com os seguintes
 * filtros:
 * <ul>
 * <li>Mídia específica</li>
 * <li>Episódio específico</li>
 * <li>Rating mínimo</li>
 * <li>Período de datas (from/to)</li>
 * </ul>
 *
 * @since 1.0
 * @see ListWatchEntriesUseCase
 * @see WatchEntryRepositoryPort
 */
public class ListWatchEntriesService implements ListWatchEntriesUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListWatchEntriesService.class);

    private final WatchEntryRepositoryPort repo;

    public ListWatchEntriesService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista as entradas de visualização de um usuário com filtros opcionais.
     *
     * @param userId    o identificador do usuário (obrigatório)
     * @param mediaId   filtro opcional por mídia específica
     * @param episodeId filtro opcional por episódio específico
     * @param minRating filtro opcional por rating mínimo (0-10)
     * @param from      filtro opcional por data inicial
     * @param to        filtro opcional por data final
     * @param pageable  parâmetros de paginação
     * @return resultado paginado com as entradas de visualização
     * @throws IllegalArgumentException se userId for nulo
     */
    @Override
    @Observed(name = "watchentry.list", contextualName = "list-watchentries-service")
    @Cacheable(value = "watchEntriesPage", key = "#userId + '_' + #mediaId + '_' + #episodeId + '_' + #minRating + '_' + #from + '_' + #to + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Measured("cinelog.service.watchentry.list")
    @AlertIfSlow(thresholdMs = 1000)
    public PageResult<WatchEntry> execute(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("mediaId", mediaId);
        params.put("episodeId", episodeId);
        params.put("minRating", minRating);
        params.put("from", from);
        params.put("to", to);
        params.put("page", pageable.getPageNumber());
        params.put("size", pageable.getPageSize());

        log.debug("Iniciando listagem de watch entries. Parâmetros: {}", params);

        try {
            if (userId == null) {
                log.warn("Tentativa de listar watch entries sem userId");
                throw new IllegalArgumentException("userId is required");
            }

            PageResult<WatchEntry> result = repo.listByUser(userId, mediaId, episodeId, minRating, from, to, pageable);
            log.debug("Listagem de watch entries concluída. Total encontrado: {}", result.totalElements());
            return result;
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao listar watch entries: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao listar watch entries. Parâmetros: {}, Erro: {}", params, e.getMessage(), e);
            throw e;
        }
    }
}
