package com.cine.cinelog.features.episodes.web.controller;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.episodes.CreateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.DeleteEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.GetEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.ListEpisodesUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.UpdateEpisodeUseCase;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.web.dto.EpisodeCreateRequest;
import com.cine.cinelog.features.episodes.web.dto.EpisodeResponse;
import com.cine.cinelog.features.episodes.web.dto.EpisodeUpdateRequest;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import com.cine.cinelog.shared.web.dto.PageableMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador REST responsável por gerenciar operações de episódios.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover episódios.
 * 
 * <p>
 * Este controlador implementa as operações CRUD completas para episódios,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 * 
 * @since 1.0
 * @see Episode
 * @see EpisodeMapper
 */
@Tag(name = "Episodes", description = "CRUD de episódios")
@Validated
@RestController
@RequestMapping("/api/v1/episodes")
public class EpisodeController {

    /**
     * Logger para registro de operações e diagnóstico de erros.
     */
    private static final Logger log = LoggerFactory.getLogger(EpisodeController.class);

    /**
     * Caso de uso para criação de episódios.
     */
    private final CreateEpisodeUseCase createUC;

    /**
     * Caso de uso para atualização de episódios.
     */
    private final UpdateEpisodeUseCase updateUC;

    /**
     * Caso de uso para busca de episódios.
     */
    private final GetEpisodeUseCase getUC;

    /**
     * Caso de uso para listagem de episódios.
     */
    private final ListEpisodesUseCase listUC;

    /**
     * Caso de uso para remoção de episódios.
     */
    private final DeleteEpisodeUseCase deleteUC;

    /**
     * Mapper para conversão entre entidades de domínio e DTOs.
     */
    private final EpisodeMapper mapper;

    /**
     * Serviço de métricas de negócio.
     */
    private final BusinessMetricsService metricsService;

    /**
     * Construtor do controlador de episódios.
     * 
     * @param createUC       Caso de uso para criação de episódios
     * @param updateUC       Caso de uso para atualização de episódios
     * @param getUC          Caso de uso para busca de episódios
     * @param listUC         Caso de uso para listagem de episódios
     * @param deleteUC       Caso de uso para remoção de episódios
     * @param mapper         Mapper para conversão entre entidades e DTOs
     * @param metricsService Serviço de métricas de negócio
     */
    public EpisodeController(CreateEpisodeUseCase createUC, UpdateEpisodeUseCase updateUC,
            GetEpisodeUseCase getUC, ListEpisodesUseCase listUC,
            DeleteEpisodeUseCase deleteUC, EpisodeMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    /**
     * Cria um novo episódio no sistema.
     * 
     * <p>
     * Este método recebe os dados do episódio, valida as informações,
     * cria a entidade de domínio e persiste no banco de dados através do caso de
     * uso.
     * </p>
     * 
     * @param req Objeto contendo os dados do episódio a ser criado
     * @return ResponseEntity contendo o episódio criado com status 201 (Created) e
     *         URI no header Location
     * @throws IllegalArgumentException se os dados fornecidos forem inválidos
     */
    @Operation(summary = "Cria um episódio")
    @PostMapping
    @Measured("cinelog.controller.episode.create")
    @AuditableAction(module = "EPISODE", action = "CREATE", description = "Criação de episódio via API")
    @SecureOperation(module = "EPISODE", value = "CONTENT_ADMIN")
    public ResponseEntity<EpisodeResponse> create(@Valid @RequestBody EpisodeCreateRequest req) {
        log.debug("Iniciando create. Parâmetros: {}", Map.of("name", req.name(), "seasonId", req.seasonId()));
        try {
            Episode created = createUC.execute(mapper.toDomain(req));
            metricsService.incrementEpisodeCreated();
            log.info("Episódio criado com sucesso. ID: {}", created.getId());
            log.debug("Finalizando create. Resultado: {}", created);
            return ResponseEntity.created(URI.create("/api/episodes/" + created.getId()))
                    .body(mapper.toResponse(created));
        } catch (Exception e) {
            log.error("Erro ao criar episódio. Parâmetros: {}. Erro: {}",
                    Map.of("name", req.name(), "seasonId", req.seasonId()), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Atualiza um episódio existente.
     * 
     * <p>
     * Este método recebe o identificador do episódio e os novos dados,
     * valida as informações e atualiza o episódio através do caso de uso.
     * </p>
     * 
     * @param id  Identificador único do episódio a ser atualizado
     * @param req Objeto contendo os dados atualizados do episódio
     * @return ResponseEntity contendo o episódio atualizado com status 200 (OK)
     * @throws IllegalArgumentException                    se o ID ou dados
     *                                                     fornecidos forem
     *                                                     inválidos
     * @throws jakarta.persistence.EntityNotFoundException se o episódio não for
     *                                                     encontrado
     */
    @Operation(summary = "Atualiza um episódio")
    @PutMapping("/{id}")
    @Measured("cinelog.controller.episode.update")
    @AuditableAction(module = "EPISODE", action = "UPDATE", description = "Atualização de episódio via API")
    public ResponseEntity<EpisodeResponse> update(@PathVariable Long id,
            @Valid @RequestBody EpisodeUpdateRequest req) {
        log.debug("Iniciando update. Parâmetros: {}", Map.of("id", id, "name", req.name()));
        try {
            Episode updated = updateUC.execute(id, mapper.toDomain(req));
            log.info("Episódio atualizado com sucesso. ID: {}", id);
            log.debug("Finalizando update. Resultado: {}", updated);
            return ResponseEntity.ok(mapper.toResponse(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar episódio. Parâmetros: {}. Erro: {}",
                    Map.of("id", id, "name", req.name()), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Busca um episódio específico pelo seu identificador único.
     * 
     * <p>
     * Este método recupera os detalhes completos de um episódio
     * através do seu ID no banco de dados.
     * </p>
     * 
     * @param id Identificador único do episódio a ser buscado
     * @return ResponseEntity contendo o episódio encontrado com status 200 (OK)
     * @throws jakarta.persistence.EntityNotFoundException se o episódio não for
     *                                                     encontrado
     */
    @Operation(summary = "Busca episódio por id")
    @GetMapping("/{id}")
    public ResponseEntity<EpisodeResponse> getById(@PathVariable Long id) {
        log.debug("Iniciando getById. Parâmetros: {}", Map.of("id", id));
        try {
            Episode episode = getUC.execute(id);
            log.debug("Finalizando getById. Resultado: episódio encontrado ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(episode));
        } catch (Exception e) {
            log.error("Erro ao buscar episódio. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lista episódios com suporte a paginação e ordenação.
     * 
     * <p>
     * Este método retorna uma lista paginada de episódios, permitindo
     * controlar o número de itens por página e a ordenação dos resultados.
     * </p>
     * 
     * @param pageable Objeto de paginação contendo informações de página, tamanho e
     *                 ordenação.
     *                 Padrão: 20 itens por página, ordenados por ID
     * @return ResponseEntity contendo a página de episódios com status 200 (OK)
     */
    @Operation(summary = "Lista episódios")
    @GetMapping
    public ResponseEntity<PageResponse<EpisodeResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando list. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Episode> result = listUC.execute(query);
            log.debug("Finalizando list. Resultado: {} episódios encontrados", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar episódios. Parâmetros: {}. Erro: {}",
                    Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Remove um episódio do sistema.
     * 
     * <p>
     * Este método realiza a exclusão lógica ou física de um episódio
     * através do seu identificador único.
     * </p>
     * 
     * @param id Identificador único do episódio a ser removido
     * @return ResponseEntity vazio com status 204 (No Content) em caso de sucesso
     * @throws jakarta.persistence.EntityNotFoundException se o episódio não for
     *                                                     encontrado
     */
    @Operation(summary = "Remove um episódio")
    @DeleteMapping("/{id}")
    @Measured("cinelog.controller.episode.delete")
    @AuditableAction(module = "EPISODE", action = "DELETE", description = "Exclusão de episódio via API")
    @SecureOperation(module = "EPISODE", value = "CONTENT_ADMIN")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Iniciando delete. Parâmetros: {}", Map.of("id", id));
        try {
            deleteUC.execute(id);
            log.info("Episódio removido com sucesso. ID: {}", id);
            log.debug("Finalizando delete");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover episódio. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }
}
