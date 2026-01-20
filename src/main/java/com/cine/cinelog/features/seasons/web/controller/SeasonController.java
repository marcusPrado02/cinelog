package com.cine.cinelog.features.seasons.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.season.*;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.web.dto.SeasonCreateRequest;
import com.cine.cinelog.features.seasons.web.dto.SeasonResponse;
import com.cine.cinelog.features.seasons.web.dto.SeasonUpdateRequest;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import com.cine.cinelog.shared.web.dto.PageableMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações de temporadas de séries.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover temporadas.
 * 
 * <p>
 * Este controlador implementa operações CRUD completas para temporadas,
 * incluindo paginação para listagem e validação de dados de entrada.
 * Todos os endpoints requerem autenticação.
 * </p>
 * 
 * @since 1.0
 * @see Season
 * @see SeasonMapper
 */
@Tag(name = "Seasons")
@Validated
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/seasons")
public class SeasonController {

    private static final Logger log = LoggerFactory.getLogger(SeasonController.class);

    private final CreateSeasonUseCase createUC;
    private final UpdateSeasonUseCase updateUC;
    private final GetSeasonUseCase getUC;
    private final ListSeasonsUseCase listUC;
    private final DeleteSeasonUseCase deleteUC;
    private final SeasonMapper mapper;
    private final BusinessMetricsService metricsService;

    public SeasonController(CreateSeasonUseCase createUC, UpdateSeasonUseCase updateUC,
            GetSeasonUseCase getUC, ListSeasonsUseCase listUC,
            DeleteSeasonUseCase deleteUC, SeasonMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Cria uma temporada")
    @PostMapping
    @Measured("cinelog.controller.season.create")
    @AuditableAction(module = "SEASON", action = "CREATE", description = "Criação de temporada via API")
    @SecureOperation(module = "SEASON", value = "CONTENT_ADMIN")
    public ResponseEntity<SeasonResponse> create(@Valid @RequestBody SeasonCreateRequest req) {
        log.debug("Iniciando create. Parâmetros: {}", Map.of("seasonNumber", req.seasonNumber()));
        try {
            Season created = createUC.execute(mapper.toDomain(req));
            metricsService.incrementSeasonCreated();
            log.info("Temporada criada com sucesso. ID: {}", created.getId());
            return ResponseEntity.created(URI.create("/api/seasons/" + created.getId()))
                    .body(mapper.toResponse(created));
        } catch (Exception e) {
            log.error("Erro ao criar temporada. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza uma temporada")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.season.update")
    @AuditableAction(module = "SEASON", action = "UPDATE", description = "Atualização de temporada via API")
    public ResponseEntity<SeasonResponse> update(@PathVariable Long id,
            @Valid @RequestBody SeasonUpdateRequest req) {
        log.debug("Iniciando update. Parâmetros: {}", Map.of("id", id));
        try {
            Season updated = updateUC.execute(id, mapper.toDomain(req));
            log.info("Temporada atualizada com sucesso. ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar temporada. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca temporada por id")
    @GetMapping("/{id}")
    public ResponseEntity<SeasonResponse> getById(@PathVariable Long id) {
        log.debug("Iniciando getById. Parâmetros: {}", Map.of("id", id));
        try {
            Season season = getUC.execute(id);
            log.debug("Temporada encontrada. ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(season));
        } catch (Exception e) {
            log.error("Erro ao buscar temporada. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista temporadas")
    @GetMapping
    public ResponseEntity<PageResponse<SeasonResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Season> result = listUC.execute(query);
            log.debug("Lista retornada. Total: {}", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar temporadas. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove uma temporada")
    @DeleteMapping("/{id}")
    @Measured("cinelog.controller.season.delete")
    @AuditableAction(module = "SEASON", action = "DELETE", description = "Exclusão de temporada via API")
    @SecureOperation(module = "SEASON", value = "CONTENT_ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Iniciando delete. Parâmetros: {}", Map.of("id", id));
        try {
            deleteUC.execute(id);
            log.info("Temporada removida com sucesso. ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover temporada. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
