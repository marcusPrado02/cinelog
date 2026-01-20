package com.cine.cinelog.core.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
/**
 * Representa Genre no domínio do sistema.
 *
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a genre.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.
 * </p>
 *
 * @since 1.0
 */
@Setter
public class Genre extends Auditable {
    private Long id;
    private String name;

}
