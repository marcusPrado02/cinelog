package com.cine.cinelog.features.users.web.dto;

import java.time.LocalDateTime;

/**
 * DTO de resposta contendo informações completas do usuário.
 * 
 * <p>
 * Retorna todos os dados do usuário incluindo informações de auditoria:
 * <ul>
 * <li>id: identificador único do usuário</li>
 * <li>name: nome do usuário</li>
 * <li>email: email do usuário</li>
 * <li>createdAt: data/hora de criação do registro</li>
 * <li>updatedAt: data/hora da última atualização</li>
 * <li>createdBy: identificador de quem criou o registro</li>
 * <li>updatedBy: identificador de quem fez a última atualização</li>
 * <li>version: versão do registro para controle de concorrência otimista</li>
 * </ul>
 * 
 * @since 1.0
 */
public record UserResponse(
                Long id,
                String name,
                String email,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                Long createdBy,
                Long updatedBy,
                Long version) {
}