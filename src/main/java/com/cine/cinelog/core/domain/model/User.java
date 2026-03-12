package com.cine.cinelog.core.domain.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa um usuário no domínio do sistema.
 *
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a
 * usuários,
 * incluindo informações básicas de identificação e autenticação.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.
 * </p>
 *
 * @since 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User extends Auditable {
    private Long id;
    private String name;
    private String email;

    public User updateFrom(User updated) {
        if (updated.getName() != null)
            this.name = updated.getName();
        if (updated.getEmail() != null)
            this.email = updated.getEmail();
        return this;
    }

}
