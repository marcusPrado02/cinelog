package com.cine.cinelog.features.people.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.person.*;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.web.dto.PersonCreateRequest;
import com.cine.cinelog.features.people.web.dto.PersonResponse;
import com.cine.cinelog.features.people.web.dto.PersonUpdateRequest;
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
 * Controlador REST responsável por gerenciar cadastro de pessoas.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover pessoas
 * (atores, diretores, produtores e demais profissionais do cinema/TV).
 *
 * <p>
 * Este controlador implementa operações CRUD completas para pessoas,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 *
 * @since 1.0
 * @see Person
 * @see PersonMapper
 */
@Tag(name = "People", description = "CRUD de pessoas")
@Validated
@RestController
@RequestMapping("/api/v1/people")
public class PersonController {

    private static final Logger log = LoggerFactory.getLogger(PersonController.class);

    private final CreatePersonUseCase createUC;
    private final UpdatePersonUseCase updateUC;
    private final GetPersonUseCase getUC;
    private final ListPeopleUseCase listUC;
    private final DeletePersonUseCase deleteUC;
    private final SearchPeopleUseCase searchUC;
    private final PersonMapper mapper;
    private final BusinessMetricsService metricsService;

    public PersonController(CreatePersonUseCase createUC, UpdatePersonUseCase updateUC,
            GetPersonUseCase getUC, ListPeopleUseCase listUC,
            DeletePersonUseCase deleteUC, SearchPeopleUseCase searchUC,
            PersonMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.searchUC = searchUC;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Cria uma pessoa")
    @PostMapping
    @Measured("cinelog.controller.person.create")
    @AuditableAction(module = "PERSON", action = "CREATE", description = "Criação de pessoa via API")
    @SecureOperation(module = "PERSON", value = "CONTENT_ADMIN")
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonCreateRequest req) {
        log.debug("Iniciando create. Parâmetros: {}", Map.of("name", req.name()));
        try {
            Person created = createUC.execute(mapper.toDomain(req));
            metricsService.incrementPersonCreated();
            log.info("Pessoa criada com sucesso. ID: {}, Nome: {}", created.getId(), req.name());
            return ResponseEntity.created(URI.create("/api/people/" + created.getId()))
                    .body(mapper.toResponse(created));
        } catch (Exception e) {
            log.error("Erro ao criar pessoa. Nome: {}, Erro: {}", req.name(), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza uma pessoa")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.person.update")
    @AuditableAction(module = "PERSON", action = "UPDATE", description = "Atualização de pessoa via API")
    public ResponseEntity<PersonResponse> update(@PathVariable Long id,
            @Valid @RequestBody PersonUpdateRequest req) {
        log.debug("Iniciando update. Parâmetros: {}", Map.of("id", id, "name", req.name()));
        try {
            Person updated = updateUC.execute(id, mapper.toDomain(req));
            log.info("Pessoa atualizada com sucesso. ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar pessoa. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca pessoa por id")
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getById(@PathVariable Long id) {
        log.debug("Iniciando getById. Parâmetros: {}", Map.of("id", id));
        try {
            Person person = getUC.execute(id);
            log.debug("Pessoa encontrada. ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(person));
        } catch (Exception e) {
            log.error("Erro ao buscar pessoa. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista pessoas")
    @GetMapping
    public ResponseEntity<PageResponse<PersonResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Person> result = listUC.execute(query);
            log.debug("Lista retornada. Total: {}", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar pessoas. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove uma pessoa")
    @DeleteMapping("/{id}")
    @Measured("cinelog.controller.person.delete")
    @AuditableAction(module = "PERSON", action = "DELETE", description = "Exclusão de pessoa via API")
    @SecureOperation(module = "PERSON", value = "CONTENT_ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Iniciando delete. Parâmetros: {}", Map.of("id", id));
        try {
            deleteUC.execute(id);
            log.info("Pessoa removida com sucesso. ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover pessoa. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Pesquisa pessoas por nome (substring, case-insensitive)")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<PersonResponse>> search(
            @RequestParam String name,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando search. Parâmetros: {}", Map.of("name", name));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Person> result = searchUC.execute(name, query);
            log.debug("Search retornado. Total: {}", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao pesquisar pessoas. Nome: {}, Erro: {}", name, e.getMessage(), e);
            throw e;
        }
    }
}
