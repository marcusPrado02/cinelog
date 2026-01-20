package com.cine.cinelog.features.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para registro de novo usuário no sistema.
 * 
 * <p>
 * Contém os dados necessários para criar uma nova conta:
 * <ul>
 * <li>name: nome completo do usuário (máx. 120 caracteres)</li>
 * <li>email: email único do usuário (máx. 255 caracteres)</li>
 * <li>password: senha com mínimo de 6 caracteres (será armazenada como
 * hash)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record RegisterRequest(
                @NotBlank @Size(max = 120) String name,
                @NotBlank @Email @Size(max = 255) String email,
                @NotBlank @Size(min = 6, max = 100) String password) {
}
