package com.cine.cinelog.core.domain.vo;

import java.math.BigDecimal;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

/**
 * Value Object para nota de avaliação (rating).
 *
 * Regras:
 * - Inteiro entre 0 e 10 (inclusive).
 */
public final class Rating {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.TEN;

    private final BigDecimal value;

    private Rating(BigDecimal value) {
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
            throw DomainException.of(ErrorCode.RATING_OUT_OF_RANGE, MIN, MAX, value);
        }
        this.value = value;
    }

    public static Rating of(BigDecimal raw) {
        if (raw == null) {
            return null;
        }
        return new Rating(raw);
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
