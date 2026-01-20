package com.cine.cinelog.core.domain.model.tmdb;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um gênero do TMDb.
 *
 * Em geral o TMDb separa listas por tipo (filme/série), mas aqui podemos
 * associar um MediaType opcional para indicar a origem.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbGenre {

    private Integer id;

    private String name;

    /**
     * Tipo ao qual esse gênero está associado (MOVIE/TV),
     * quando aplicável. Pode ser null se não fizer diferença.
     */
    private MediaType type;
}
