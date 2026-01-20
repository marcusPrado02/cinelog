package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa um episódio de uma temporada de série no domínio do sistema.
 * 
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a
 * episódios,
 * incluindo número do episódio, nome, data de exibição e relacionamento com a
 * temporada pai.
 * Cada episódio pertence a uma temporada (Season).
 * </p>
 * 
 * @since 1.0
 * @see Season
 * @see Media
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Episode extends Auditable {
    private Long id;
    private Long seasonId;
    private Integer episodeNumber;
    private String name;
    private LocalDate airDate;

    public Episode updateFrom(Episode patch) {
        if (patch == null) {
            return this;
        }
        this.seasonId = patch.seasonId != null ? patch.seasonId : this.seasonId;
        this.episodeNumber = patch.episodeNumber != null ? patch.episodeNumber : this.episodeNumber;
        this.name = patch.name != null ? patch.name : this.name;
        this.airDate = patch.airDate != null ? patch.airDate : this.airDate;
        return this;
    }

}
