package com.cine.cinelog.features.episodes.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

import com.cine.cinelog.shared.persistence.AuditableEntity;

@Entity
@Table(name = "episodes", uniqueConstraints = @UniqueConstraint(name = "uk_episode_season_number", columnNames = {
/**
 * Entidade JPA que representa a tabela de episode no banco de dados.
 * Mapeia a estrutura de persistência para Episode.
 * 
 * <p>Esta entidade contém as anotações JPA necessárias para mapeamento objeto-relacional
 * e é convertida para/de Episode pelo EpisodeMapper.</p>
 * 
 * @since 1.0
 * @see Episode
 */
        "season_id", "episode_number" }))
public class EpisodeEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;
    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;
    @Column(length = 200)
    private String name;
    @Column(name = "air_date")
    private LocalDate airDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Long s) {
        this.seasonId = s;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer n) {
        this.episodeNumber = n;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public LocalDate getAirDate() {
        return airDate;
    }

    public void setAirDate(LocalDate d) {
        this.airDate = d;
    }
}