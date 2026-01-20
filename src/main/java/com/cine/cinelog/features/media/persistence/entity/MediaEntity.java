package com.cine.cinelog.features.media.persistence.entity;

import jakarta.persistence.*;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.shared.persistence.AuditableEntity;

/**
 * Entidade JPA que representa a tabela "media" no banco de dados.
 *
 * Esta classe mapeia os campos de persistência (colunas) e cuida de
 * informações de auditoria simples (createdAt, updatedAt) através dos
 * callbacks @PrePersist/@PreUpdate.
 *
 * Observação: a conversão entre `MediaEntity` e o modelo de domínio
 * `Media` é feita pelo mapper `MediaEntityMapper`.
 */
@Entity
/**
 * Entidade JPA que representa a tabela de media no banco de dados.
 * Mapeia a estrutura de persistência para Media.
 *
 * <p>
 * Esta entidade contém as anotações JPA necessárias para mapeamento
 * objeto-relacional
 * e é convertida para/de Media pelo MediaMapper.
 * </p>
 *
 * @since 1.0
 * @see Media
 */
@Table(name = "media")
public class MediaEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType type;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "original_title", length = 300)
    private String originalTitle;

    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    @Column(name = "poster_url", length = 300)
    private String posterUrl;

    @Column(name = "backdrop_url", length = 300)
    private String backdropUrl;

    @Lob
    private String overview;

    @Column(name = "tmdb_id")
    private Long tmdbId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }
}
