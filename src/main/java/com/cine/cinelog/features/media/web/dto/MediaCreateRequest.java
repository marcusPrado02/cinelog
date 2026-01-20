package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO de requisição para criação de nova mídia (filme ou série).
 *
 * <p>
 * Contém todos os dados necessários para cadastrar uma mídia no sistema:
 * <ul>
 * <li>title: título da mídia (obrigatório, máx. 300 caracteres)</li>
 * <li>type: tipo de mídia - MOVIE ou SERIES (obrigatório)</li>
 * <li>releaseYear: ano de lançamento (entre 1888 e 3000)</li>
 * <li>originalTitle: título original (máx. 300 caracteres)</li>
 * <li>originalLanguage: idioma original (máx. 10 caracteres)</li>
 * <li>posterUrl: URL da imagem do poster (máx. 300 caracteres)</li>
 * <li>backdropUrl: URL da imagem de fundo (máx. 300 caracteres)</li>
 * <li>overview: sinopse/descrição da mídia</li>
 * <li>tmdbId: ID da mídia no TMDB (The Movie Database)</li>
 * </ul>
 *
 * <p>
 * Validações: título e tipo são obrigatórios. O ano de lançamento, quando
 * fornecido, deve estar entre 1888 (primeiro filme) e 3000.
 *
 * @since 1.0
 */
@Schema(name = "MediaCreateRequest")
public class MediaCreateRequest {
    @Schema(description = "Título da mídia", example = "Inception")
    @NotBlank
    @Size(max = 300)
    private String title;
    @Schema(description = "Tipo de mídia", example = "MOVIE")
    @NotNull
    private MediaType type;
    @Schema(description = "Ano de lançamento", example = "2010")
    @Min(1888)
    @Max(3000)
    private Integer releaseYear;
    @Schema(description = "Título original", example = "Inception")
    @Size(max = 300)
    private String originalTitle;
    @Schema(description = "Idioma original", example = "Inglês")
    @Size(max = 10)
    private String originalLanguage;
    @Schema(description = "URL do pôster", example = "https://example.com/poster.jpg")
    @Size(max = 300)
    private String posterUrl;
    @Schema(description = "URL do fundo", example = "https://example.com/backdrop.jpg")
    @Size(max = 300)
    private String backdropUrl;
    @Schema(description = "Sinopse", example = "Um ladrão que invade os sonhos das pessoas.")
    private String overview;

    @Schema(description = "ID do TMDB", example = "27205")
    private Long tmdbId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType v) {
        type = v;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer v) {
        releaseYear = v;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String v) {
        originalTitle = v;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String v) {
        originalLanguage = v;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String v) {
        posterUrl = v;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String v) {
        backdropUrl = v;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String v) {
        overview = v;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }
}
