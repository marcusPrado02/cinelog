package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa uma temporada de série no domínio do sistema.
 * 
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a
 * temporadas,
 * incluindo número da temporada, nome, data de exibição e relacionamento com a
 * mídia pai.
 * Cada temporada pertence a uma série (Media) e pode conter múltiplos
 * episódios.
 * </p>
 * 
 * @since 1.0
 * @see Media
 * @see Episode
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Season extends Auditable {
    private Long id;
    private Long mediaId;
    private Integer seasonNumber;
    private String name;
    private LocalDate airDate;

    public Season updateFrom(Season patch) {
        if (patch == null) {
            return this;
        }
        this.mediaId = patch.mediaId != null ? patch.mediaId : this.mediaId;
        this.seasonNumber = patch.seasonNumber != null ? patch.seasonNumber : this.seasonNumber;
        this.name = patch.name != null ? patch.name : this.name;
        this.airDate = patch.airDate != null ? patch.airDate : this.airDate;
        return this;
    }

}
