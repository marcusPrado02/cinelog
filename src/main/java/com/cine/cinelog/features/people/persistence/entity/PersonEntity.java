package com.cine.cinelog.features.people.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

import com.cine.cinelog.shared.persistence.AuditableEntity;

@Entity
/**
 * Entidade JPA que representa a tabela de person no banco de dados.
 * Mapeia a estrutura de persistência para Person.
 *
 * <p>
 * Esta entidade contém as anotações JPA necessárias para mapeamento
 * objeto-relacional
 * e é convertida para/de Person pelo PersonMapper.
 * </p>
 *
 * @since 1.0
 * @see Person
 */
@Table(name = "people", indexes = @Index(name = "idx_people_name", columnList = "name"))
public class PersonEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;
    @Column(name = "place_of_birth", length = 200)
    private String placeOfBirth;

    @Column(name = "tmdb_person_id", unique = true)
    private Long tmdbPersonId;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "known_for_department", length = 100)
    private String knownForDepartment;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "popularity")
    private Double popularity;

    @Column(name = "imdb_id", length = 20)
    private String imdbId;

    @Column(name = "homepage", length = 500)
    private String homepage;

    @Column(name = "deathday")
    private LocalDate deathday;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate b) {
        this.birthDate = b;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String p) {
        this.placeOfBirth = p;
    }

    public Long getTmdbPersonId() {
        return tmdbPersonId;
    }

    public void setTmdbPersonId(Long tmdbPersonId) {
        this.tmdbPersonId = tmdbPersonId;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getKnownForDepartment() {
        return knownForDepartment;
    }

    public void setKnownForDepartment(String knownForDepartment) {
        this.knownForDepartment = knownForDepartment;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Double getPopularity() {
        return popularity;
    }

    public void setPopularity(Double popularity) {
        this.popularity = popularity;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public LocalDate getDeathday() {
        return deathday;
    }

    public void setDeathday(LocalDate deathday) {
        this.deathday = deathday;
    }
}
