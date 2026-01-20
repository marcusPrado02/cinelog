package com.cine.cinelog.core.domain.vo;

import java.util.Objects;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

/**
 * Value Object para título de mídia.
 *
 * Regras:
 * - Não pode ser nulo ou em branco (M1)
 * - É normalizado com trim()
 * - Tamanho máximo: 200 caracteres (M2)
 *
 * Qualquer violação lança DomainException com ErrorCode específico,
 * e a mensagem final é resolvida via i18n.
 */
public final class Title {

    private static final int MAX_LENGTH = 200;

    private final String value;

    private Title(String value) {
        this.value = value;
    }

    public static Title of(String raw) {
        String v = raw == null ? "" : raw.trim();

        if (v.isEmpty()) {
            // M1: título obrigatório
            throw DomainException.of(ErrorCode.MEDIA_TITLE_REQUIRED);
        }

        if (v.length() > MAX_LENGTH) {
            // M2: título muito longo (passamos MAX_LENGTH como argumento para i18n se
            // quiser)
            throw DomainException.of(ErrorCode.MEDIA_TITLE_TOO_LONG, MAX_LENGTH);
        }

        return new Title(v);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Title t))
            return false;
        return value.equalsIgnoreCase(t.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toLowerCase());
    }
}
