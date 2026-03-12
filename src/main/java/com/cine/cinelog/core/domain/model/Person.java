package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
/**
 * Representa Person no domínio do sistema.
 *
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a person.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.
 * </p>
 *
 * @since 1.0
 */
@Setter
public class Person extends Auditable {
    private Long id;
    private String name;
    private LocalDate birthDate;
    private String placeOfBirth;
    private Long tmdbPersonId;

}
