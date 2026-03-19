package com.cine.cinelog.features.media.web.dto;

import java.util.List;

import com.cine.cinelog.core.domain.enums.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de requisição para busca avançada de mídias com múltiplos critérios.
 *
 * <p>
 * Permite buscar mídias utilizando diversos filtros e parâmetros de paginação:
 * <ul>
 * <li>text: busca textual em título, overview e nomes de pessoas associadas</li>
 * <li>type: filtrar por tipo de mídia (MOVIE ou SERIES)</li>
 * <li>yearMin/yearMax: filtrar por intervalo de ano de lançamento</li>
 * <li>ratingMin/ratingMax: filtrar por intervalo de avaliação</li>
 * <li>genreIds: lista de IDs de gêneros para filtrar</li>
 * <li>page: número da página (começa em 0)</li>
 * <li>size: quantidade de itens por página (padrão: 10)</li>
 * <li>sortBy: campo para ordenação (ex: "releaseYear", "title")</li>
 * <li>sortDirection: direção da ordenação (ASC ou DESC)</li>
 * </ul>
 *
 * <p>
 * Todos os filtros são opcionais e podem ser combinados. A busca retorna
 * resultados paginados.
 *
 * @since 1.0
 */
@Getter
@Setter
public class MediaSearchRequest {

    @Schema(description = "Texto de busca (título, overview, pessoas)")
    private String text;

    @Schema(description = "Tipo da mídia: FILM ou SERIES")
    private MediaType type;

    @Min(value = 1888, message = "yearMin deve ser ≥ 1888")
    @Schema(description = "Ano mínimo de lançamento")
    private Integer yearMin;

    @Max(value = 2200, message = "yearMax deve ser ≤ 2200")
    @Schema(description = "Ano máximo de lançamento")
    private Integer yearMax;

    @DecimalMin(value = "0.0", message = "ratingMin deve ser ≥ 0.0")
    @DecimalMax(value = "10.0", message = "ratingMin deve ser ≤ 10.0")
    @Schema(description = "Nota mínima")
    private Double ratingMin;

    @DecimalMin(value = "0.0", message = "ratingMax deve ser ≥ 0.0")
    @DecimalMax(value = "10.0", message = "ratingMax deve ser ≤ 10.0")
    @Schema(description = "Nota máxima")
    private Double ratingMax;

    @Schema(description = "IDs de gêneros para filtrar")
    private List<Long> genreIds;

    @Min(value = 0, message = "page deve ser ≥ 0")
    @Schema(description = "Página atual (0 = primeira)", defaultValue = "0")
    private Integer page = 0;

    @Min(value = 1, message = "size deve ser ≥ 1")
    @Max(value = 100, message = "size deve ser ≤ 100")
    @Schema(description = "Tamanho da página", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Campo para ordenar", example = "releaseYear")
    private String sortBy;

    @Schema(description = "Direção de ordenação", example = "ASC")
    private String sortDirection;
}
