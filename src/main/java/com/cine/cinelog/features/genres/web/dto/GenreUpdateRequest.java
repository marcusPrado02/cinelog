package com.cine.cinelog.features.genres.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização de gênero existente.
 * 
 * <p>
 * Permite atualizar o nome do gênero.
 * 
 * <p>
 * Validações:
 * <ul>
 * <li>name: obrigatório, não pode ser vazio, máximo de 100 caracteres</li>
 * </ul>
 * 
 * @since 1.0
 */
public record GenreUpdateRequest(@NotBlank @Size(max = 100) String name) {
}