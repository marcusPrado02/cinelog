package com.cine.cinelog.features.people.web.dto;

import java.time.LocalDate;

/**
 * DTO de resposta contendo informações completas de uma pessoa.
 *
 * @since 1.0
 */
public record PersonResponse(
        Long id,
        String name,
        LocalDate birthDate,
        String placeOfBirth,
        String profileUrl,
        String biography,
        String knownForDepartment,
        Integer gender,
        Double popularity,
        String imdbId,
        String homepage,
        LocalDate deathday) {
}
