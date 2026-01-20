package com.cine.cinelog.features.users.web.dto;

import jakarta.validation.constraints.*;

/**
 * DTO de requisição para criação de novo usuário.
 * 
 * <p>
 * Contém os dados básicos necessários para criar um usuário:
 * <ul>
 * <li>name: nome do usuário (máx. 120 caracteres)</li>
 * <li>email: email único do usuário (máx. 255 caracteres)</li>
 * </ul>
 * 
 * <p>
 * Nota: A senha não é incluída aqui pois usuários são criados através de
 * RegisterRequest na autenticação.
 * 
 * @since 1.0
 */
public record UserCreateRequest(
                @NotBlank @Size(max = 120) String name,
                @NotBlank @Email @Size(max = 255) String email) {
}
