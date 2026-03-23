package com.cine.cinelog.features.media.web.controller;

import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Controlador REST para criacao de midia por administradores.
 *
 * <p>Persiste a midia no banco via {@link CreateMediaUseCase}.</p>
 */
@Tag(name = "Admin Media", description = "Gerenciamento de midia para administradores")
@RestController
@RequestMapping("/api/v1/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaController {

    private static final Logger log = LoggerFactory.getLogger(AdminMediaController.class);

    private final CreateMediaUseCase createMediaUseCase;
    private final MediaMapper mapper;
    private final BusinessMetricsService metricsService;

    public AdminMediaController(CreateMediaUseCase createMediaUseCase,
                                MediaMapper mapper,
                                BusinessMetricsService metricsService) {
        this.createMediaUseCase = createMediaUseCase;
        this.mapper = mapper;
        this.metricsService = metricsService;
    }

    @PostMapping
    @Operation(summary = "Criar midia (admin)")
    @ApiResponse(responseCode = "201", description = "Midia criada com sucesso")
    @Measured("cinelog.controller.admin.media.create")
    @AuditableAction(module = "ADMIN_MEDIA", action = "CREATE", description = "Criacao de midia via API admin")
    @SecureOperation(module = "ADMIN_MEDIA", value = "MEDIA_ADMIN")
    public ResponseEntity<MediaResponse> create(@Valid @RequestBody MediaCreateRequest request) {
        log.debug("Admin criando midia: title={} type={}", request.getTitle(), request.getType());

        Media domain = mapper.toDomain(request);
        Media saved = createMediaUseCase.execute(domain);

        metricsService.incrementMediaCreated(request.getType().toString());
        log.info("Midia criada por admin: id={} title={}", saved.getId(), saved.getTitle());

        return ResponseEntity
                .created(URI.create("/api/v1/media/" + saved.getId()))
                .body(mapper.toResponse(saved));
    }
}
