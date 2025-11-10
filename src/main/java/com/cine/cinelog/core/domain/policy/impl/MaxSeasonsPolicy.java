package com.cine.cinelog.core.domain.policy.impl;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

public final class MaxSeasonsPolicy {
    private final int max;

    private MaxSeasonsPolicy(int max) {
        this.max = max;
    }

    public static MaxSeasonsPolicy of(int max) {
        if (max < 1)
            throw DomainException.of(ErrorCode.GEN_VALIDATION, "max seasons inválido");
        return new MaxSeasonsPolicy(max);
    }

    public void ensureWithinLimit(int seasonsCount) {
        if (seasonsCount > max) {
            throw DomainException.of(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Série excede limite de temporadas: " + seasonsCount + " > " + max);
        }
    }
}