package com.cine.cinelog.features.genres.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para criação de novo gênero.
 * 
 * <p>
 * Contém o nome do gênero a ser cadastrado (ex: "Ação", "Drama", "Comédia").
 * 
 * <p>
 * Validações:
 * <ul>
 * <li>name: obrigatório, não pode ser vazio, máximo de 100 caracteres</li>
 * </ul>
 * 
 * @since 1.0
 */
public record GenreCreateRequest(@NotBlank @Size(max = 100) String name) {
}