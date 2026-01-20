package com.cine.cinelog.features.media.web;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.application.services.MediaSearchService;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.enums.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para busca avançada de mídias.
 *
 * <p>
 * <strong>Feature:</strong> MediaSearch (PR6 - Specification Pattern)
 *
 * <p>
 * Endpoints de busca flexível com múltiplos critérios:
 * <ul>
 * <li>GET /api/media/search - Busca avançada com todos os filtros</li>
 * <li>GET /api/media/search/text - Busca simples por texto</li>
 * </ul>
 *
 * <p>
 * <strong>Exemplos de uso:</strong>
 * 
 * <pre>
 * // Busca por texto
 * GET /api/media/search/text?q=Star Wars&page=0&size=20
 *
 * // Filmes de 2020-2023 com rating > 8.0
 * GET /api/media/search?type=MOVIE&yearMin=2020&yearMax=2023&ratingMin=8.0
 *
 * // Séries de ação (genreId=1) ordenadas por rating
 * GET /api/media/search?type=SERIES&genreIds=1&sort=averageRating&direction=DESC
 *
 * // Busca combinada
 * GET /api/media/search?text=Matrix&type=MOVIE&ratingMin=7.0&genreIds=1,5
 * </pre>
 *
 * @since 1.0 (PR6)
 * @see MediaSearchService
 * @see MediaSearchCriteria
 */
@RestController
@RequestMapping("/api/media")
@Tag(name = "Media Search", description = "Busca avançada de filmes e séries")
public class MediaSearchController {

    private final MediaSearchService mediaSearchService;

    public MediaSearchController(MediaSearchService mediaSearchService) {
        this.mediaSearchService = mediaSearchService;
    }

    /**
     * Busca avançada de mídias com múltiplos critérios.
     *
     * <p>
     * Suporta busca combinada com múltiplos filtros usando Specification Pattern.
     * Todos os filtros são opcionais e aplicados com AND lógico.
     *
     * @param text      texto livre para buscar em título/descrição (OR)
     * @param type      tipo de mídia (MOVIE ou SERIES)
     * @param yearMin   ano mínimo de lançamento (inclusive)
     * @param yearMax   ano máximo de lançamento (inclusive)
     * @param ratingMin rating mínimo (inclusive)
     * @param ratingMax rating máximo (inclusive)
     * @param genreIds  lista de IDs de gêneros (OR entre eles)
     * @param page      número da página (0-based, padrão: 0)
     * @param size      tamanho da página (1-100, padrão: 20)
     * @param sort      campo de ordenação (padrão: id)
     * @param direction direção (ASC/DESC, padrão: ASC)
     * @return resultado paginado de mídias
     */
    @GetMapping("/search")
    @Operation(summary = "Busca avançada de mídias", description = "Busca flexível com múltiplos critérios usando Specification Pattern. "
            +
            "Todos os filtros são opcionais e combinados com AND lógico. " +
            "Texto busca em título e descrição (OR). " +
            "Gêneros são combinados com OR.", responses = {
                    @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso", content = @Content(schema = @Schema(implementation = PageResult.class))),
                    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
            })
    public ResponseEntity<PageResult<Media>> advancedSearch(
            @Parameter(description = "Texto livre (busca em título e descrição)", example = "Star Wars") @RequestParam(required = false) String text,

            @Parameter(description = "Tipo de mídia (MOVIE ou SERIES)", example = "MOVIE") @RequestParam(required = false) MediaType type,

            @Parameter(description = "Ano mínimo de lançamento (inclusive)", example = "2020") @RequestParam(required = false) Integer yearMin,

            @Parameter(description = "Ano máximo de lançamento (inclusive)", example = "2023") @RequestParam(required = false) Integer yearMax,

            @Parameter(description = "Rating mínimo (0.0 - 10.0, inclusive)", example = "8.0") @RequestParam(required = false) Double ratingMin,

            @Parameter(description = "Rating máximo (0.0 - 10.0, inclusive)", example = "10.0") @RequestParam(required = false) Double ratingMax,

            @Parameter(description = "IDs de gêneros (separados por vírgula, OR lógico)", example = "1,5,12") @RequestParam(required = false) List<Long> genreIds,

            @Parameter(description = "Número da página (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamanho da página (1-100)", example = "20") @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Campo de ordenação", example = "averageRating") @RequestParam(defaultValue = "id") String sort,

            @Parameter(description = "Direção de ordenação (ASC/DESC)", example = "DESC") @RequestParam(defaultValue = "ASC") String direction) {
        // Construir critérios
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setText(text);
        criteria.setType(type);
        criteria.setYearMin(yearMin);
        criteria.setYearMax(yearMax);
        criteria.setRatingMin(ratingMin);
        criteria.setRatingMax(ratingMax);
        criteria.setGenreIds(genreIds);

        // Executar busca
        PageQuery pageQuery = new PageQuery(page, size, sort, direction);
        PageResult<Media> result = mediaSearchService.search(criteria, pageQuery);

        return ResponseEntity.ok(result);
    }

    /**
     * Busca simples por texto livre.
     *
     * @param query texto para buscar (título ou descrição)
     * @param page  número da página (0-based)
     * @param size  tamanho da página (1-100)
     * @return resultado paginado de mídias
     */
    @GetMapping("/search/text")
    @Operation(summary = "Busca simples por texto", description = "Busca por texto livre em título e descrição. " +
            "Útil para busca rápida sem filtros adicionais.", responses = {
                    @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso", content = @Content(schema = @Schema(implementation = PageResult.class))),
                    @ApiResponse(responseCode = "400", description = "Query vazia ou inválida")
            })
    public ResponseEntity<PageResult<Media>> searchByText(
            @Parameter(description = "Texto para buscar", example = "Matrix", required = true) @RequestParam String q,

            @Parameter(description = "Número da página (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamanho da página (1-100)", example = "20") @RequestParam(defaultValue = "20") int size) {
        PageQuery pageQuery = new PageQuery(page, size, "id", "ASC");
        PageResult<Media> result = mediaSearchService.searchByText(q, pageQuery);

        return ResponseEntity.ok(result);
    }
}
