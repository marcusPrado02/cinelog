package com.cine.cinelog.core.domain.model;

import com.cine.cinelog.core.domain.enums.Role;
import com.cine.cinelog.shared.persistence.AuditableEntity;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Representa um crédito (participação) de uma pessoa em uma mídia no domínio do
 * sistema.
 * 
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a
 * créditos,
 * estabelecendo o relacionamento entre pessoas (atores, diretores, etc.) e
 * mídias.
 * Inclui informações sobre o papel desempenhado, nome do personagem e ordem de
 * exibição.
 * </p>
 * 
 * @since 1.0
 * @see Person
 * @see Media
 * @see Role
 */
@Getter
@Setter
public class Credit extends Auditable {
    private Long id;
    private Long mediaId;
    private Long personId;
    private Role role;
    private String characterName;
    private Short orderIndex;

}