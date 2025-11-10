package com.cine.cinelog.core.domain.model;

import java.util.Objects;

import com.cine.cinelog.core.domain.enums.MediaType;
import lombok.Getter;
import lombok.Setter;

/**
 * Representa a entidade de domínio 'Media' usada pela camada de domínio
 * e pelos casos de uso da aplicação.
 *
 * Comentários em português explicam os campos e o propósito desta classe
 * para facilitar a leitura e manutenção por desenvolvedores que preferem
 * documentação em português.
 */

@Getter
@Setter
public class Media {
    private Long id;
    private String title;
    private MediaType type;
    private Integer releaseYear;
    private String originalTitle;
    private String originalLanguage;
    private String posterUrl;
    private String backdropUrl;
    private String overview;

    public Media(Long id, String title, MediaType type, Integer releaseYear,
            String originalTitle, String originalLanguage,
            String posterUrl, String backdropUrl, String overview) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.releaseYear = releaseYear;
        this.originalTitle = originalTitle;
        this.originalLanguage = originalLanguage;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.overview = overview;
        validateInvariants();
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
                overview);
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
}
