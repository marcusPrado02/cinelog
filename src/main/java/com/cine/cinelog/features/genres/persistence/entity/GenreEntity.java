package com.cine.cinelog.features.genres.persistence.entity;

import com.cine.cinelog.shared.persistence.AuditableEntity;

import jakarta.persistence.*;

@Entity
/**
 * Entidade JPA que representa a tabela de genre no banco de dados.
 * Mapeia a estrutura de persistência para Genre.
 * 
 * <p>Esta entidade contém as anotações JPA necessárias para mapeamento objeto-relacional
 * e é convertida para/de Genre pelo GenreMapper.</p>
 * 
 * @since 1.0
 * @see Genre
 */
@Table(name = "genres", uniqueConstraints = @UniqueConstraint(name = "uk_genres_name", columnNames = "name"))
public class GenreEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

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
}