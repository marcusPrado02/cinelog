package com.cine.cinelog.features.people.web.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * DTO de requisição para cadastro de nova pessoa (ator, diretor, etc.).
 * 
 * <p>
 * Contém os dados básicos da pessoa:
 * <ul>
 * <li>name: nome completo da pessoa (obrigatório, máx. 200 caracteres)</li>
 * <li>birthDate: data de nascimento (opcional)</li>
 * <li>placeOfBirth: local de nascimento (opcional, máx. 200 caracteres)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record PersonCreateRequest(
        @NotBlank @Size(max = 200) String name,
        LocalDate birthDate,
        @Size(max = 200) String placeOfBirth) {
}
