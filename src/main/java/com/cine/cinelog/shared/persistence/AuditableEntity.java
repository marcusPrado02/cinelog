package com.cine.cinelog.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@MappedSuperclass
/**
 * Entidade JPA que representa a tabela de auditable no banco de dados.
 * Mapeia a estrutura de persistência para Auditable.
 * 
 * <p>Esta entidade contém as anotações JPA necessárias para mapeamento objeto-relacional
 * e é convertida para/de Auditable pelo AuditableMapper.</p>
 * 
 * @since 1.0
 * @see Auditable
 */
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    protected Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    protected Long updatedBy;

    @Column(name = "version", nullable = false)
    protected Long version;
}