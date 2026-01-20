package com.cine.cinelog.features.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para autenticação de usuário (login).
 * 
 * <p>
 * Contém as credenciais necessárias para autenticar um usuário no sistema:
 * <ul>
 * <li>Email: identificador único do usuário</li>
 * <li>Password: senha em texto plano (será validada contra hash no
 * backend)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record LoginRequest(
                @NotBlank @Email String email,
                @NotBlank String password) {
}
