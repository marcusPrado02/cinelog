package com.cine.cinelog.features.users.web.dto;

import jakarta.validation.constraints.*;

/**
 * DTO de requisição para atualização de dados do usuário.
 * 
 * <p>
 * Permite atualizar apenas o nome do usuário. O campo é obrigatório quando
 * fornecido na requisição.
 * 
 * <p>
 * Validações:
 * <ul>
 * <li>name: obrigatório, não pode ser vazio, máximo de 120 caracteres</li>
 * </ul>
 * 
 * @since 1.0
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 120) String name) {
}