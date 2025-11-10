package com.cine.cinelog.core.domain.vo;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

public final class Year {
    private final int value;

    private Year(int value) {
        int current = java.time.Year.now().getValue();
        if (value < 1888 || value > current + 2) {
            throw DomainException.of(ErrorCode.MEDIA_YEAR_OUT_OF_RANGE,
                    "Ano inválido: " + value + " (permitido 1888.." + (current + 2) + ")");
        }
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Year of(Integer v) {
        if (v == null)
            throw DomainException.of(ErrorCode.GEN_VALIDATION, "Ano é obrigatório");
        return new Year(v);
    }
}