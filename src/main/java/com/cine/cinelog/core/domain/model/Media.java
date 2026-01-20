package com.cine.cinelog.core.domain.model;

import java.util.Objects;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Representa uma mídia (filme ou série) no domínio do sistema.
 * 
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a mídias,
 * incluindo informações como título, ano de lançamento, idioma, imagens e
 * metadados.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.
 * </p>
 * 
 * <p>
 * As mídias podem ser de dois tipos: MOVIE (filmes) ou TV_SHOW (séries).
 * Para séries, é possível associar temporadas e episódios.
 * </p>
 * 
 * @since 1.0
 * @see MediaType
 * @see Season
 */
@Getter
@Setter
public class Media extends Auditable {
    private Long id;
    private String title;
    private MediaType type;
    private Integer releaseYear;
    private String originalTitle;
    private String originalLanguage;
    private String posterUrl;
    private String backdropUrl;
    private String overview;

    private Long tmdbId;

    public Media(Long id, String title, MediaType type, Integer releaseYear,
            String originalTitle, String originalLanguage,
            String posterUrl, String backdropUrl, String overview, Long tmdbId) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.releaseYear = releaseYear;
        this.originalTitle = originalTitle;
        this.tmdbId = tmdbId;
        this.originalLanguage = originalLanguage;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.overview = overview;
        validateInvariants();
    }

    public Media() {
    }

    public void validateInvariants() {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title is required");
        if (type == null)
            throw new IllegalArgumentException("type is required");
        if (releaseYear != null && (releaseYear < 1888 || releaseYear > 3000))
            throw new IllegalArgumentException("releaseYear out of range");
    }

    public Media withId(Long id) {
        return new Media(id, title, type, releaseYear, originalTitle, originalLanguage, posterUrl, backdropUrl,
                overview, tmdbId);
    }

    public Media updateFrom(Media patch) {
        if (patch == null)
            return this;

        this.title = (patch.title != null) ? patch.title : this.title;
        this.type = (patch.type != null) ? patch.type : this.type;
        this.releaseYear = (patch.releaseYear != null) ? patch.releaseYear : this.releaseYear;
        this.originalTitle = (patch.originalTitle != null) ? patch.originalTitle : this.originalTitle;
        this.originalLanguage = (patch.originalLanguage != null) ? patch.originalLanguage : this.originalLanguage;
        this.posterUrl = (patch.posterUrl != null) ? patch.posterUrl : this.posterUrl;
        this.backdropUrl = (patch.backdropUrl != null) ? patch.backdropUrl : this.backdropUrl;
        this.overview = (patch.overview != null) ? patch.overview : this.overview;
        this.tmdbId = (patch.tmdbId != null) ? patch.tmdbId : this.tmdbId;
        return this;
    }

    public void normalize() {
        if (this.title != null) {
            this.title = this.title.trim();
        }
        if (this.originalTitle != null) {
            this.originalTitle = this.originalTitle.trim();
        }
    }

    public boolean hasTmdbId() {
        return tmdbId != null;
    }

    public boolean isMovie() {
        return MediaType.MOVIE.equals(this.type);
    }

    public boolean isSeries() {
        return MediaType.SERIES.equals(this.type);
    }

}
