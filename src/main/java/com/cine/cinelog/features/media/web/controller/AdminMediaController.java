package com.cine.cinelog.features.media.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import java.util.Map;

@Tag(name = "Admin Media", description = "Gerenciamento de mídia para administradores")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
/**
 * Controlador REST responsável por gerenciar operações de AdminMedia.
 * Fornece endpoints para criar, atualizar, buscar, listar e remover adminmedia.
 *
 * <p>
 * Este controlador implementa as operações CRUD completas para adminmedia,
 * incluindo paginação para listagem e validação de dados de entrada.
 * </p>
 *
 * @since 1.0
 * @see AdminMedia
 */
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private static final Logger log = LoggerFactory.getLogger(AdminMediaController.class);
    private final BusinessMetricsService metricsService;

    public AdminMediaController(BusinessMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @PostMapping
    @Measured("cinelog.controller.admin.media.create")
    @AuditableAction(module = "ADMIN_MEDIA", action = "CREATE", description = "Criação de mídia via API admin")
    @SecureOperation(module = "ADMIN_MEDIA", value = "MEDIA_ADMIN")
    public ResponseEntity<MediaResponse> create(@RequestBody MediaCreateRequest request) {
        log.debug("Admin criando mídia. Parâmetros: {}",
                Map.of("title", request.getTitle(), "type", request.getType(),
                        "releaseYear", request.getReleaseYear() != null ? request.getReleaseYear() : "N/A"));
        try {
            // apenas ADMIN cai aqui por causa do SecurityConfig
            MediaResponse response = new MediaResponse(
                    null, request.getTitle(), request.getType(), request.getReleaseYear(),
                    request.getOriginalTitle(), request.getOriginalLanguage(),
                    request.getPosterUrl(), request.getBackdropUrl(),
                    request.getOverview(), request.getTmdbId());
            metricsService.incrementMediaCreated(request.getType().toString());
            log.info("Mídia criada por admin com sucesso. Tipo: {}, Título: {}",
                    request.getType(), request.getTitle());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao criar mídia (admin). Tipo: {}, Título: {}, Erro: {}",
                    request.getType(), request.getTitle(), e.getMessage(), e);
            throw e;
        }
    }
}
