package com.cine.cinelog.core.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
/**
 * Representa Auditable no domínio do sistema.
 *
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a
 * auditable.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.
 * </p>
 *
 * @since 1.0
 */
@Setter
public abstract class Auditable implements Serializable {
    protected LocalDateTime createdAt;

    protected LocalDateTime updatedAt;

    protected Long createdBy;

    protected Long updatedBy;

    protected Long version;
}
