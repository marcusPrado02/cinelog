package com.cine.cinelog.core.domain.model.tmdb;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Créditos de uma mídia no TMDb: elenco (cast) e equipe técnica (crew).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbCredits {

    private Long tmdbId;

    private List<CastMember> cast;

    private List<CrewMember> crew;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastMember {

        private Long tmdbPersonId;

        private String name;

        /**
         * Nome do personagem interpretado.
         */
        private String character;

        /**
         * Ordem de importância na lista de elenco (menor = mais importante).
         */
        private Integer castOrder;

        /**
         * URL absoluta da foto de perfil (quando existir).
         */
        private String profileUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrewMember {

        private Long tmdbPersonId;

        private String name;

        /**
         * Função exercida (ex.: "Director", "Writer").
         */
        private String job;

        private String department;

        /**
         * URL absoluta da foto de perfil (quando existir).
         */
        private String profileUrl;
    }
}
