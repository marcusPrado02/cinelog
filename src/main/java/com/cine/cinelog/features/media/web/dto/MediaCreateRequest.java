package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO usado para criar ou atualizar uma mídia via API.
 *
 * Possui validações Jakarta Bean Validation para garantir dados mínimos
 * antes de chegar à camada de aplicação.
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
}
