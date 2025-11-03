package com.cine.cinelog.features.genres.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "genres", uniqueConstraints = @UniqueConstraint(name = "uk_genres_name", columnNames = "name"))
public class GenreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, length = 100)
    private String name;

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}