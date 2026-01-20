package com.cine.cinelog.features.genres.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.genre.*;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.features.genres.mapper.GenreMapper;
import com.cine.cinelog.features.genres.web.dto.GenreCreateRequest;
import com.cine.cinelog.features.genres.web.dto.GenreResponse;
import com.cine.cinelog.features.genres.web.dto.GenreUpdateRequest;
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

import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações de gêneros de mídia.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover gêneros.
 * 
 * <p>
 * Este controlador implementa operações CRUD completas para gêneros,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 * 
 * @since 1.0
 * @see Genre
 * @see GenreMapper
 */
@Tag(name = "Genres", description = "CRUD de gêneros")
@Validated
@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private static final Logger log = LoggerFactory.getLogger(GenreController.class);

    private final CreateGenreUseCase createUC;
    private final UpdateGenreUseCase updateUC;
    private final GetGenreUseCase getUC;
    private final ListGenresUseCase listUC;
    private final DeleteGenreUseCase deleteUC;
    private final GenreMapper mapper;
    private final BusinessMetricsService metricsService;

    public GenreController(CreateGenreUseCase createUC, UpdateGenreUseCase updateUC,
            GetGenreUseCase getUC, ListGenresUseCase listUC,
            DeleteGenreUseCase deleteUC, GenreMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Cria um gênero")
    @PostMapping
    @Measured("cinelog.controller.genre.create")
    @AuditableAction(module = "GENRE", action = "CREATE", description = "Criação de gênero via API")
    @SecureOperation(module = "GENRE", value = "CONTENT_ADMIN")
    public ResponseEntity<GenreResponse> create(@Valid @RequestBody GenreCreateRequest req) {
        log.debug("Iniciando create. Parâmetros: {}", Map.of("name", req.name()));
        try {
            Genre created = createUC.execute(mapper.toDomain(req));
            metricsService.incrementGenreCreated();
            log.info("Gênero criado com sucesso. ID: {}", created.getId());
            log.debug("Finalizando create. Resultado: {}", created);
            return ResponseEntity.created(URI.create("/api/genres/" + created.getId()))
                    .body(mapper.toResponse(created));
        } catch (Exception e) {
            log.error("Erro ao criar gênero. Parâmetros: {}. Erro: {}", Map.of("name", req.name()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza um gênero")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.genre.update")
    @AuditableAction(module = "GENRE", action = "UPDATE", description = "Atualização de gênero via API")
    public ResponseEntity<GenreResponse> update(@PathVariable Long id,
            @Valid @RequestBody GenreUpdateRequest req) {
        log.debug("Iniciando update. Parâmetros: {}", Map.of("id", id, "name", req.name()));
        try {
            Genre updated = updateUC.execute(id, mapper.toDomain(req));
            metricsService.incrementGenreUpdated();
            log.info("Gênero atualizado com sucesso. ID: {}", id);
            log.debug("Finalizando update. Resultado: {}", updated);
            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar gênero. Parâmetros: {}. Erro: {}", Map.of("id", id, "name", req.name()),
                    e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca um gênero por id")
    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getById(@PathVariable Long id) {
        log.debug("Iniciando getById. Parâmetros: {}", Map.of("id", id));
        try {
            Genre genre = getUC.execute(id);
            log.debug("Finalizando getById. Resultado: gênero encontrado ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(genre));
        } catch (Exception e) {
            log.error("Erro ao buscar gênero. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista gêneros")
    @GetMapping
    public ResponseEntity<PageResponse<GenreResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Genre> result = listUC.execute(query);
            log.debug("Finalizando list. Resultado: {} gêneros encontrados", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar gêneros. Parâmetros: {}. Erro: {}",
                    Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove um gênero")
    @DeleteMapping("/{id}")
    @Measured("cinelog.controller.genre.delete")
    @AuditableAction(module = "GENRE", action = "DELETE", description = "Exclusão de gênero via API")
    @SecureOperation(module = "GENRE", value = "CONTENT_ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Iniciando delete. Parâmetros: {}", Map.of("id", id));
        try {
            deleteUC.execute(id);
            metricsService.incrementGenreDeleted();
            log.info("Gênero removido com sucesso. ID: {}", id);
            log.debug("Finalizando delete");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover gênero. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }
}
