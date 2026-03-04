package com.cine.cinelog.features.watchentry.web.controller;

import com.cine.cinelog.core.application.ports.in.watchentry.CreateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.DeleteWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.GetWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.ListWatchEntriesUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.UpdateWatchEntryUseCase;
import com.cine.cinelog.features.watchentry.mapper.WatchEntryMapper;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryCreateRequest;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryResponse;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

/**
 * Controlador REST responsável por gerenciar o histórico de visualizações e
 * avaliações de usuários.
 * Fornece endpoints para registrar, atualizar, buscar, listar e remover
 * entradas do histórico
 * de mídias assistidas pelos usuários, incluindo notas e comentários.
 *
 * <p>
 * Este controlador implementa operações CRUD completas para o histórico de
 * visualizações,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 *
 * @since 1.0
 * @see com.cine.cinelog.core.domain.model.WatchEntry
 * @see WatchEntryMapper
 */
@Tag(name = "Watch Entries", description = "Histórico de assistidos e avaliações")
@Validated
@RestController
@RequestMapping("/api/v1/watch-entries")
public class WatchEntryController {

    private static final Logger log = LoggerFactory.getLogger(WatchEntryController.class);

    private final CreateWatchEntryUseCase createUC;
    private final ListWatchEntriesUseCase listUC;
    private final UpdateWatchEntryUseCase updateUC;
    private final GetWatchEntryUseCase getUC;
    private final DeleteWatchEntryUseCase deleteUC;
    private final WatchEntryMapper WatchEntryDtoMapper;
    private final BusinessMetricsService metricsService;

    public WatchEntryController(CreateWatchEntryUseCase createUC, ListWatchEntriesUseCase listUC,
            UpdateWatchEntryUseCase updateUC, GetWatchEntryUseCase getUC, DeleteWatchEntryUseCase deleteUC,
            WatchEntryMapper WatchEntryDtoMapper, BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.listUC = listUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.deleteUC = deleteUC;
        this.WatchEntryDtoMapper = WatchEntryDtoMapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Cria um registro de watch")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Measured("cinelog.controller.watchentry.create")
    @AuditableAction(module = "WATCH_ENTRY", action = "CREATE", description = "Criação de registro de visualização via API")
    public ResponseEntity<WatchEntryResponse> create(@Valid @RequestBody WatchEntryCreateRequest req) {
        log.debug("Iniciando create watch entry");
        try {
            var saved = createUC.execute(WatchEntryDtoMapper.toDomain(req));

            // Métricas de negócio
            metricsService.incrementWatchEntryCreated("GENERAL");
            if (saved.getRating() != null) {
                metricsService.incrementRatingGiven(saved.getRating().intValue());
            }

            log.info("Watch entry criado com sucesso. ID: {}", saved.getId());
            return ResponseEntity.created(URI.create("/api/watch/" + saved.getId()))
                    .body(WatchEntryDtoMapper.toResponse(saved));
        } catch (Exception e) {
            log.error("Erro ao criar watch entry. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista registros do usuário (paginado)")
    @GetMapping
    @Measured("cinelog.controller.watchentry.list")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<PageResponse<WatchEntryResponse>> list(
            @RequestParam Long userId,
            @RequestParam(required = false) Long mediaId,
            @RequestParam(required = false) Long episodeId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Listando watch entries. UserId: {}, page: {}, size: {}", userId, page, size);
        try {
            var result = listUC.execute(userId, mediaId, episodeId, minRating, from, to, PageRequest.of(page, size));
            log.debug("Watch entries listados com sucesso");
            return ResponseEntity.ok(PageResponseMapper.from(result, WatchEntryDtoMapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar watch entries. UserId: {}. Erro: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza rating/comentário/metadata")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.watchentry.update")
    @AuditableAction(module = "WATCH_ENTRY", action = "UPDATE", description = "Atualização de registro de visualização via API")
    public ResponseEntity<WatchEntryResponse> update(@PathVariable Long id,
            @Valid @RequestBody WatchEntryCreateRequest req) {
        log.debug("Atualizando watch entry. ID: {}", id);
        try {
            var updated = updateUC.execute(id, WatchEntryDtoMapper.toDomain(req), true);
            log.info("Watch entry atualizado com sucesso. ID: {}", id);
            return ResponseEntity.ok(WatchEntryDtoMapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar watch entry. ID: {}. Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove um registro")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Measured("cinelog.controller.watchentry.delete")
    @AuditableAction(module = "WATCH_ENTRY", action = "DELETE", description = "Exclusão de registro de visualização via API")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Removendo watch entry. ID: {}", id);
        try {
            deleteUC.execute(id);

            log.info("Watch entry removido com sucesso. ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover watch entry. ID: {}. Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Obtém um registro pelo ID")
    @GetMapping("/{id}")
    @Measured("cinelog.controller.watchentry.get")
    public ResponseEntity<WatchEntryResponse> getById(@PathVariable Long id) {
        log.debug("Buscando watch entry. ID: {}", id);
        try {
            var entry = getUC.execute(id);
            log.debug("Watch entry encontrado. ID: {}", id);
            return ResponseEntity.ok(WatchEntryDtoMapper.toResponse(entry));
        } catch (Exception e) {
            log.error("Erro ao buscar watch entry. ID: {}. Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
