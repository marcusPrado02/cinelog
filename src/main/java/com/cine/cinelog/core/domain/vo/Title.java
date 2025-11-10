package com.cine.cinelog.core.domain.vo;

import java.util.Objects;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

public final class Title {
    private final String value;

    private Title(String value) {
        this.value = value;
    }

    public static Title of(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw DomainException.of(ErrorCode.MEDIA_TITLE_REQUIRED, "Título é obrigatório");
        }
        if (v.length() > 200) {
            throw DomainException.of(ErrorCode.MEDIA_TITLE_TOO_LONG, "Título excede 200 caracteres");
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
        return (o instanceof Title t) && t.value.equalsIgnoreCase(this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toLowerCase());
    }
}