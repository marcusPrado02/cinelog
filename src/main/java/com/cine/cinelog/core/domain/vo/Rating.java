package com.cine.cinelog.core.domain.vo;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

public final class Rating {
    private final double value;

    public Rating(double value) {
        if (Double.isNaN(value) || value < 0.0 || value > 10.0) {
            throw DomainException.of(ErrorCode.RATING_OUT_OF_RANGE, "Rating deve estar entre 0 e 10");
        }
        this.value = value;
    }

    public double value() {
        return value;
    }

    public static Rating of(Double v) {
        if (v == null)
            return new Rating(0.0);
        return new Rating(v);
    }
}
