package com.cine.cinelog.features.people.web.dto;

import java.time.LocalDate;

/**
 * DTO de resposta contendo informações completas de uma pessoa.
 * 
 * <p>
 * Retorna todos os dados cadastrados da pessoa:
 * <ul>
 * <li>id: identificador único da pessoa</li>
 * <li>name: nome completo</li>
 * <li>birthDate: data de nascimento</li>
 * <li>placeOfBirth: local de nascimento</li>
 * </ul>
 * 
 * @since 1.0
 */
public record PersonResponse(Long id, String name, LocalDate birthDate, String placeOfBirth) {
}