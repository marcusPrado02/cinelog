package com.cine.cinelog.features.credits.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.credits.*;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.web.dto.CreditCreateRequest;
import com.cine.cinelog.features.credits.web.dto.CreditResponse;
import com.cine.cinelog.features.credits.web.dto.CreditUpdateRequest;
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
 * Controlador REST responsável por gerenciar créditos de mídias.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover créditos
 * que representam a participação de pessoas em mídias (atores, diretores, etc).
 * 
 * <p>
 * Este controlador implementa operações CRUD completas para créditos,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 * 
 * @since 1.0
 * @see Credit
 * @see CreditMapper
 */
@Tag(name = "Credits", description = "CRUD de créditos")
@Validated
@RestController
@RequestMapping("/api/v1/credits")
public class CreditController {

    private static final Logger log = LoggerFactory.getLogger(CreditController.class);

    private final CreateCreditUseCase createUC;
    private final UpdateCreditUseCase updateUC;
    private final GetCreditUseCase getUC;
    private final ListCreditsUseCase listUC;
    private final DeleteCreditUseCase deleteUC;
    private final CreditMapper mapper;
    private final BusinessMetricsService metricsService;

    public CreditController(CreateCreditUseCase createUC, UpdateCreditUseCase updateUC,
            GetCreditUseCase getUC, ListCreditsUseCase listUC,
            DeleteCreditUseCase deleteUC, CreditMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Cria um crédito")
    @PostMapping
    @Measured("cinelog.controller.credit.create")
    @AuditableAction(module = "CREDIT", action = "CREATE", description = "Criação de crédito via API")
    @SecureOperation(module = "CREDIT", value = "CONTENT_ADMIN")
    public ResponseEntity<CreditResponse> create(@Valid @RequestBody CreditCreateRequest req) {
        log.debug("Iniciando create. Parâmetros: {}", Map.of("role", req.role()));
        try {
            Credit created = createUC.execute(mapper.toDomain(req));
            metricsService.incrementCreditCreated(req.role());
            log.info("Crédito criado com sucesso. ID: {}, Role: {}", created.getId(), req.role());
            return ResponseEntity.created(URI.create("/api/credits/" + created.getId()))
                    .body(mapper.toResponse(created));
        } catch (Exception e) {
            log.error("Erro ao criar crédito. Role: {}, Erro: {}", req.role(), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza um crédito")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.credit.update")
    @AuditableAction(module = "CREDIT", action = "UPDATE", description = "Atualização de crédito via API")
    public ResponseEntity<CreditResponse> update(@PathVariable Long id,
            @Valid @RequestBody CreditUpdateRequest req) {
        log.debug("Iniciando update. Parâmetros: {}", Map.of("id", id, "role", req.role()));
        try {
            Credit updated = updateUC.execute(id, mapper.toDomain(req));
            log.info("Crédito atualizado com sucesso. ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar crédito. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca crédito por id")
    @GetMapping("/{id}")
    public ResponseEntity<CreditResponse> getById(@PathVariable Long id) {
        log.debug("Iniciando getById. Parâmetros: {}", Map.of("id", id));
        try {
            Credit credit = getUC.execute(id);
            log.debug("Crédito encontrado. ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(credit));
        } catch (Exception e) {
            log.error("Erro ao buscar crédito. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista créditos")
    @GetMapping
    public ResponseEntity<PageResponse<CreditResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Credit> result = listUC.execute(query);
            log.debug("Lista retornada. Total: {}", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar créditos. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove um crédito")
    @DeleteMapping("/{id}")
    @Measured("cinelog.controller.credit.delete")
    @AuditableAction(module = "CREDIT", action = "DELETE", description = "Exclusão de crédito via API")
    @SecureOperation(module = "CREDIT", value = "CONTENT_ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Iniciando delete. Parâmetros: {}", Map.of("id", id));
        try {
            deleteUC.execute(id);
            log.info("Crédito removido com sucesso. ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover crédito. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
