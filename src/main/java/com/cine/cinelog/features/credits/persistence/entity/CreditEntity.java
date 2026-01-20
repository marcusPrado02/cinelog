package com.cine.cinelog.features.credits.persistence.entity;

import com.cine.cinelog.shared.persistence.AuditableEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "credits", indexes = {
        @Index(name = "idx_credits_media_role", columnList = "media_id, role"),
        @Index(name = "idx_credits_person_role", columnList = "person_id, role")
/**
 * Entidade JPA que representa a tabela de credit no banco de dados.
 * Mapeia a estrutura de persistência para Credit.
 * 
 * <p>Esta entidade contém as anotações JPA necessárias para mapeamento objeto-relacional
 * e é convertida para/de Credit pelo CreditMapper.</p>
 * 
 * @since 1.0
 * @see Credit
 */
})
public class CreditEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(nullable = false, columnDefinition = "ENUM('DIRECTOR','WRITER','ACTOR','PRODUCER','COMPOSER')")
    private String role;

    @Column(name = "character_name", length = 200)
    private String characterName;
    @Column(name = "order_index")
    private Short orderIndex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long m) {
        this.mediaId = m;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long p) {
        this.personId = p;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String r) {
        this.role = r;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String c) {
        this.characterName = c;
    }

    public Short getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Short o) {
        this.orderIndex = o;
    }
}