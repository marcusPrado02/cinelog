package com.cine.cinelog.features.watchlist.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;

import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.watchlist.AddToWatchlistUseCase;
import com.cine.cinelog.core.application.ports.in.watchlist.ListMyWatchlistUseCase;
import com.cine.cinelog.core.application.ports.in.watchlist.RemoveFromWatchlistUseCase;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.features.watchlist.mapper.WatchlistMapper;
import com.cine.cinelog.features.watchlist.web.dto.WatchlistAddRequest;
import com.cine.cinelog.features.watchlist.web.dto.WatchlistResponse;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import com.cine.cinelog.shared.web.dto.PageableMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import java.util.Map;

@Tag(name = "Watchlist", description = "Gerenciamento da lista de desejos do usuário")
@Validated
@RestController
/**
 * Controlador REST responsável por gerenciar operações de Watchlist.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover watchlist.
 * 
 * <p>
 * Este controlador implementa as operações CRUD completas para watchlist,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 * 
 * @since 1.0
 * @see Watchlist
 */
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {

    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final AddToWatchlistUseCase addUseCase;
    private final RemoveFromWatchlistUseCase removeUseCase;
    private final ListMyWatchlistUseCase listUseCase;
    private final WatchlistMapper mapper;
    private final BusinessMetricsService metricsService;

    public WatchlistController(AddToWatchlistUseCase addUseCase,
            RemoveFromWatchlistUseCase removeUseCase,
            ListMyWatchlistUseCase listUseCase,
            WatchlistMapper mapper,
            BusinessMetricsService metricsService) {
        this.addUseCase = addUseCase;
        this.removeUseCase = removeUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Adiciona um item à watchlist do usuário")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Measured("cinelog.controller.watchlist.add")
    @AuditableAction(module = "WATCHLIST", action = "ADD", description = "Adição à watchlist via API")
    public ResponseEntity<WatchlistResponse> add(@Valid @RequestBody WatchlistAddRequest request) {
        log.debug("Iniciando add. Parâmetros: {}", Map.of("mediaId", request.mediaId()));
        try {
            var it = addUseCase.add(new AddToWatchlistUseCase.AddCommand(request.mediaId()));
            metricsService.incrementWatchlistAdd("MOVIE"); // Default - melhorar depois com mediaType real
            log.info("Item adicionado à watchlist. MediaID: {}", request.mediaId());
            return ResponseEntity.ok(new WatchlistResponse(it.getId(), it.getMediaId(), it.getAddedAt().toString()));
        } catch (Exception e) {
            log.error("Erro ao adicionar à watchlist. MediaID: {}, Erro: {}", request.mediaId(), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove um item da watchlist do usuário")
    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Measured("cinelog.controller.watchlist.remove")
    @AuditableAction(module = "WATCHLIST", action = "REMOVE", description = "Remoção da watchlist via API")
    public ResponseEntity<Void> remove(@PathVariable Long mediaId) {
        log.debug("Iniciando remove. Parâmetros: {}", Map.of("mediaId", mediaId));
        try {
            removeUseCase.remove(mediaId);
            metricsService.incrementWatchlistRemove();
            log.info("Item removido da watchlist. MediaID: {}", mediaId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover da watchlist. MediaID: {}, Erro: {}", mediaId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista a watchlist do usuário")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PageResponse<WatchlistResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<WatchlistItem> result = listUseCase.execute(query);
            log.debug("Lista retornada. Total: {}", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar watchlist. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
