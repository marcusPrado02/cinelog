package com.cine.cinelog.features.media.web.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.RecommendMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.SearchMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.application.query.SortDirection;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.MediaWithRating;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.features.media.web.dto.MediaSearchRequest;
import com.cine.cinelog.features.media.web.dto.MediaUpdateRequest;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import com.cine.cinelog.shared.web.dto.PageableMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações de mídias (filmes e
 * séries).
 * Fornece endpoints para criar, atualizar, buscar, listar, remover, pesquisar e
 * recomendar mídias.
 *
 * <p>
 * Este controlador implementa operações CRUD completas além de funcionalidades
 * avançadas como busca com filtros, recomendações personalizadas e paginação.
 * </p>
 *
 * @since 1.0
 * @see Media
 * @see MediaMapper
 * @see MediaSearchCriteria
 */
@Tag(name = "Media", description = "CRUD de filmes e séries")
@Validated
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final CreateMediaUseCase createUC;
    private final UpdateMediaUseCase updateUC;
    private final GetMediaUseCase getUC;
    private final SearchMediaUseCase searchUC;
    private final ListMediaUseCase listUC;
    private final DeleteMediaUseCase deleteUC;
    private final RecommendMediaUseCase recommendUC;
    private final MediaMapper mapper;
    private final BusinessMetricsService metricsService;

    public MediaController(CreateMediaUseCase createUC, UpdateMediaUseCase updateUC,
            GetMediaUseCase getUC, ListMediaUseCase listUC,
            DeleteMediaUseCase deleteUC, SearchMediaUseCase searchUC,
            RecommendMediaUseCase recommendUC, MediaMapper mapper,
            BusinessMetricsService metricsService) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.searchUC = searchUC;
        this.recommendUC = recommendUC;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    // Converte DTO para domínio, executa caso de uso e devolve DTO de
    // resposta com status 201 CREATED.
    @Operation(summary = "Cria uma mídia", description = "Cadastra um filme/série")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Measured("cinelog.controller.media.create")
    public ResponseEntity<MediaResponse> createUC(@Valid @RequestBody MediaCreateRequest req) {
        log.debug("Iniciando createUC. Parâmetros: {}", Map.of("title", req.getTitle(), "type", req.getType()));
        try {
            var saved = createUC.execute(mapper.toDomain(req));

            // Métrica de negócio: mídia criada
            metricsService.incrementMediaCreated(saved.getType().name());

            log.info("Mídia criada com sucesso. ID: {}, Título: {}", saved.getId(), saved.getTitle());
            log.debug("Finalizando createUC. Resultado: {}", saved);
            return ResponseEntity.created(URI.create("/api/v1/media/" + saved.getId()))
                    .body(mapper.toResponse(saved));
        } catch (Exception e) {
            log.error("Erro ao criar mídia. Parâmetros: {}. Erro: {}",
                    Map.of("title", req.getTitle(), "type", req.getType()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca por id")
    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> getUC(@PathVariable Long id) {
        log.debug("Iniciando getUC. Parâmetros: {}", Map.of("id", id));
        try {
            Media media = getUC.execute(id);
            log.debug("Finalizando getUC. Resultado: mídia encontrada ID: {}", id);
            return ResponseEntity.ok(mapper.toResponse(media));
        } catch (Exception e) {
            log.error("Erro ao buscar mídia. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Lista mídias", description = "Filtro por type e busca por título")
    @GetMapping
    public ResponseEntity<PageResponse<MediaResponse>> listUC(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("Iniciando listUC. Parâmetros: {}",
                Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()));
        try {
            PageQuery query = PageableMapper.toPageQuery(pageable);
            PageResult<Media> result = listUC.execute(query);
            log.debug("Finalizando listUC. Resultado: {} mídias encontradas", result.content().size());
            return ResponseEntity.ok(PageResponseMapper.from(result, mapper::toResponse));
        } catch (Exception e) {
            log.error("Erro ao listar mídias. Parâmetros: {}. Erro: {}",
                    Map.of("page", pageable.getPageNumber(), "size", pageable.getPageSize()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Atualiza uma mídia")
    @PutMapping("/{id}")
    public ResponseEntity<MediaResponse> updateUC(@PathVariable Long id, @Valid @RequestBody MediaUpdateRequest req) {
        log.debug("Iniciando updateUC. Parâmetros: {}", Map.of("id", id, "title", req.title(), "type", req.type()));
        try {
            var updateUCd = updateUC.execute(id, mapper.toDomain(req));
            log.info("Mídia atualizada com sucesso. ID: {}", id);
            log.debug("Finalizando updateUC. Resultado: {}", updateUCd);
            return ResponseEntity.ok(mapper.toResponse(updateUCd));
        } catch (Exception e) {
            log.error("Erro ao atualizar mídia. Parâmetros: {}. Erro: {}",
                    Map.of("id", id, "title", req.title(), "type", req.type()), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Remove uma mídia")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUC(@PathVariable Long id) {
        log.debug("Iniciando deleteUC. Parâmetros: {}", Map.of("id", id));
        try {
            deleteUC.execute(id);
            log.info("Mídia removida com sucesso. ID: {}", id);
            log.debug("Finalizando deleteUC");
        } catch (Exception e) {
            log.error("Erro ao remover mídia. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Busca avançada de mídias", description = "Permite filtrar por texto, tipo, ano, nota e gêneros, com paginação")
    @PostMapping("/search")
    @Measured("cinelog.controller.search_media")
    @AlertIfSlow(thresholdMs = 1000, metricName = "cinelog.slow_search")
    public ResponseEntity<PageResponse<MediaResponse>> searchUC(@Valid @RequestBody MediaSearchRequest req) {
        log.debug("Iniciando searchUC. Parâmetros: text={}, type={}, page={}",
                req.getText(), req.getType(), req.getPage());
        try {
            var criteria = new MediaSearchCriteria();
            criteria.setText(req.getText());
            criteria.setType(req.getType());
            criteria.setYearMin(req.getYearMin());
            criteria.setYearMax(req.getYearMax());
            criteria.setRatingMin(req.getRatingMin());
            criteria.setRatingMax(req.getRatingMax());
            criteria.setGenreIds(req.getGenreIds());

            var pageQuery = new PageQuery(req.getPage(), req.getSize());

            var result = searchUC.execute(criteria, pageQuery);
            var response = PageResponseMapper.from(result, mapper::toResponse);

            log.debug("Finalizando searchUC. Resultado: {} mídias encontradas", result.content().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro na busca avançada de mídias. text={}, type={}, erro={}",
                    req.getText(), req.getType(), e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Recomenda mídias para o usuário", description = "Baseado no histórico de visualizações")
    @GetMapping("/recommendations/{userId}")
    @Measured("cinelog.controller.recommend_media")
    @AlertIfSlow(thresholdMs = 1500, metricName = "cinelog.slow_recommendation")
    public ResponseEntity<List<MediaResponse>> recommendUC(@PathVariable Long userId) {
        log.debug("Iniciando recommendUC. Parâmetros: {}", Map.of("userId", userId));
        try {
            List<MediaWithRating> recommendations = recommendUC.execute(userId);
            List<MediaResponse> response = recommendations.stream()
                    .map(mediaWithRating -> {
                        Media media = new Media();
                        media.setId(mediaWithRating.getMediaId());
                        media.setTitle(mediaWithRating.getTitle());
                        media.setType(mediaWithRating.getType());
                        return mapper.toResponse(media);
                    })
                    .toList();

            log.info("Recomendações geradas para usuário {}. Total: {}", userId, response.size());
            log.debug("Finalizando recommendUC. Resultado: {} recomendações", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao gerar recomendações. Parâmetros: {}. Erro: {}", Map.of("userId", userId), e.getMessage(),
                    e);
            throw e;
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail notFound(IllegalArgumentException ex) {
        // Retorna um ProblemDetail com a mensagem da exceção para o cliente.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
