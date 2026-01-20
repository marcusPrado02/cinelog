package com.cine.cinelog.core.domain.model;

import java.math.BigDecimal;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa uma mídia com dados de rating agregados,
 * usada pelo mecanismo de recomendação.
 */
@Getter
@Setter
@AllArgsConstructor
/**
 * Representa MediaWithRating no domínio do sistema.
 * 
 * <p>Esta classe encapsula os conceitos e regras de negócio relacionados a mediawithrating.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.</p>
 * 
 * @since 1.0
 */
@NoArgsConstructor
public class MediaWithRating {

    private Long mediaId;
    private String title;
    private MediaType type;
    private BigDecimal averageRating;
    private long ratingCount;

}
