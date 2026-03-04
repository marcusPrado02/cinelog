package com.cine.cinelog.features.users.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.user.*;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.model.UserStats;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.web.dto.UserCreateRequest;
import com.cine.cinelog.features.users.web.dto.UserResponse;
import com.cine.cinelog.features.users.web.dto.UserStatsResponse;
import com.cine.cinelog.features.users.web.dto.UserUpdateRequest;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import com.cine.cinelog.shared.web.dto.PageableMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações de usuários.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover usuários,
 * além de consultar estatísticas do usuário autenticado.
 * 
 * <p>
 * Este controlador implementa operações CRUD completas para usuários,
 * incluindo paginação para listagem, validação de dados e endpoint de
 * estatísticas.
 * </p>
 * 
 * @since 1.0
 * @see User
 * @see UserMapper
 * @see UserStats
 */
@Tag(name = "Users", description = "CRUD de usuários")
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final CreateUserUseCase createUC;
    private final UpdateUserUseCase updateUC;
    private final GetUserUseCase getUC;
    private final ListUsersUseCase listUC;
    private final DeleteUserUseCase deleteUC;
    private final GetMyStatsUseCase getMyStatsUseCase;
    private final UserMapper mapper;
    private final BusinessMetricsService metricsService;

    public UserController(CreateUserUseCase createUC, UpdateUserUseCase updateUC,
            GetUserUseCase getUC, ListUsersUseCase listUC,
            DeleteUserUseCase deleteUC, GetMyStatsUseCase getMyStatsUseCase, UserMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.getMyStatsUseCase = getMyStatsUseCase;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Cria um usuário")
    @PostMapping
    @Measured("cinelog.controller.user.create")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        log.debug("Iniciando create. Parâmetros: {}", Map.of("name", req.name(), "email", req.email()));

        try {
            User created = createUC.execute(mapper.toDomain(req));

            log.info("Usuário criado com sucesso. ID: {}", created.getId());
            log.debug("Finalizando create. Resultado: {}", created);

            return ResponseEntity.created(URI.create("/api/users/" + created.getId()))
                    .body(mapper.toResponse(created));
        } catch (Exception e) {
            log.error("Erro ao criar usuário. Parâmetros: {}. Erro: {}",
                    Map.of("name", req.name(), "email", req.email()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza um usuário")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.user.update")
    @PreAuthorize("hasRole('ADMIN') or @springSecurityCurrentUserProvider.getCurrentUser().map(u -> u.id().equals(#id)).orElse(false)")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest req) {
        log.debug("Iniciando update. Parâmetros: {}", Map.of("id", id, "name", req.name()));

        try {
            User updated = updateUC.execute(id, mapper.toDomain(req));

            log.info("Usuário atualizado com sucesso. ID: {}", id);
            log.debug("Finalizando update. Resultado: {}", updated);

            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar usuário. Parâmetros: {}. Erro: {}",
                    Map.of("id", id, "name", req.name()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca usuário por id")
    @GetMapping("/{id}")
    @Measured("cinelog.controller.user.get")
    @PostAuthorize("hasRole('ADMIN') or returnObject.body.email == authentication.name")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        log.debug("Iniciando getById. Parâmetros: {}", Map.of("id", id));

        try {
            User user = getUC.execute(id);
            log.debug("Finalizando getById. Resultado: usuário encontrado ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(user));
        } catch (Exception e) {
            log.error("Erro ao buscar usuário. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista usuários")
    @GetMapping
    @Measured("cinelog.controller.user.list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));

        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<User> result = listUC.execute(query);

            log.debug("Finalizando list. Resultado: {} usuários encontrados", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar usuários. Parâmetros: {}. Erro: {}",
                    Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove um usuário")
    @DeleteMapping("/{id}")
    @Measured("cinelog.controller.user.delete")
    @SecureOperation(module = "USER", value = "USER_ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Iniciando delete. Parâmetros: {}", Map.of("id", id));

        try {
            deleteUC.execute(id);
            log.info("Usuário removido com sucesso. ID: {}", id);
            log.debug("Finalizando delete");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover usuário. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/me/stats")
    @Measured("cinelog.controller.user_stats")
    public UserStatsResponse getMyStats() {
        log.debug("Iniciando getMyStats");

        try {
            UserStats stats = getMyStatsUseCase.execute();
            log.debug("Finalizando getMyStats. Resultado: totalEntries={}", stats.getTotalEntries());
            return new UserStatsResponse(
                    stats.getTotalEntries(),
                    stats.getTotalRated(),
                    stats.getAverageRating(),
                    stats.getFirstWatchDate(),
                    stats.getLastWatchDate());
        } catch (Exception e) {
            log.error("Erro ao obter estatísticas do usuário. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
