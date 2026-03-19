package com.cine.cinelog.features.watchprogress.web;

import com.cine.cinelog.core.application.services.WatchProgressService;
import com.cine.cinelog.core.domain.vo.SeriesProgress;
import com.cine.cinelog.features.watchprogress.web.dto.UpdateProgressRequest;
import com.cine.cinelog.features.watchprogress.web.dto.WatchProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Controller REST para progresso de visualização de séries.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * <p>
 * Endpoints:
 * <ul>
 *   <li>POST /api/watchentries/{id}/progress - Criar/Atualizar progresso</li>
 *   <li>GET /api/watchentries/{id}/progress - Buscar progresso</li>
 *   <li>DELETE /api/watchentries/{id}/progress - Deletar progresso</li>
 * </ul>
 *
 * @since 1.0 (PR6 - Fase 5)
 */
@RestController
@RequestMapping("/api/watchentries")
@Tag(name = "Watch Progress", description = "Gerenciamento de progresso de visualização de séries")
public class WatchProgressController {

    private final WatchProgressService watchProgressService;

    public WatchProgressController(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    /**
     * Cria ou atualiza progresso de visualização.
     *
     * @param watchEntryId ID do watch entry (deve ser episódio)
     * @param request dados de progresso
     * @return progresso criado/atualizado
     */
    @PostMapping("/{id}/progress")
    @Operation(
            summary = "Criar/Atualizar progresso de visualização",
            description = "Registra ou atualiza o progresso de visualização de um episódio de série. " +
                    "Permite controlar temporada, episódio e tempo assistido.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Progresso atualizado com sucesso",
                            content = @Content(schema = @Schema(implementation = WatchProgressResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "201",
                            description = "Progresso criado com sucesso",
                            content = @Content(schema = @Schema(implementation = WatchProgressResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @ApiResponse(responseCode = "404", description = "Watch entry não encontrado")
            }
    )
    public ResponseEntity<WatchProgressResponse> createOrUpdateProgress(
            @Parameter(description = "ID do watch entry", example = "123", required = true)
            @PathVariable("id") Long watchEntryId,

            @Parameter(description = "Dados de progresso", required = true)
            @Valid @RequestBody UpdateProgressRequest request
    ) {
        boolean existed = watchProgressService.hasProgress(watchEntryId);

        SeriesProgress progress = new SeriesProgress(
                request.currentSeason(),
                request.currentEpisode(),
                Duration.ofSeconds(request.watchedDurationSeconds()),
                Duration.ofSeconds(request.totalDurationSeconds())
        );

        SeriesProgress saved = watchProgressService.saveProgress(watchEntryId, progress);
        WatchProgressResponse response = toResponse(watchEntryId, saved);

        HttpStatus status = existed ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Busca progresso de visualização.
     *
     * @param watchEntryId ID do watch entry
     * @return progresso se existir
     */
    @GetMapping("/{id}/progress")
    @Operation(
            summary = "Buscar progresso de visualização",
            description = "Retorna o progresso atual de visualização de um episódio, " +
                    "incluindo temporada, episódio, tempo assistido e percentual de conclusão.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Progresso encontrado",
                            content = @Content(schema = @Schema(implementation = WatchProgressResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Progresso não encontrado")
            }
    )
    public ResponseEntity<WatchProgressResponse> getProgress(
            @Parameter(description = "ID do watch entry", example = "123", required = true)
            @PathVariable("id") Long watchEntryId
    ) {
        return watchProgressService.getProgress(watchEntryId)
                .map(progress -> toResponse(watchEntryId, progress))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deleta progresso de visualização.
     *
     * @param watchEntryId ID do watch entry
     * @return 204 No Content
     */
    @DeleteMapping("/{id}/progress")
    @Operation(
            summary = "Deletar progresso de visualização",
            description = "Remove o progresso de visualização de um episódio. " +
                    "Usado quando a série é completada ou abandonada.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Progresso deletado com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Watch entry não encontrado")
            }
    )
    public ResponseEntity<Void> deleteProgress(
            @Parameter(description = "ID do watch entry", example = "123", required = true)
            @PathVariable("id") Long watchEntryId
    ) {
        watchProgressService.deleteProgress(watchEntryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Converte SeriesProgress para DTO de resposta.
     */
    private WatchProgressResponse toResponse(Long watchEntryId, SeriesProgress progress) {
        return new WatchProgressResponse(
                watchEntryId,
                progress.getCurrentSeason(),
                progress.getCurrentEpisode(),
                progress.getWatchedDuration().toSeconds(),
                progress.getTotalDuration().toSeconds(),
                progress.getCompletionPercentage(),
                progress.isEpisodeCompleted(),
                progress.format()
        );
    }
}
