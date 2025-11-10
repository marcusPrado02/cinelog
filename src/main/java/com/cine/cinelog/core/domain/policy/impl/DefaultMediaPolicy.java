package com.cine.cinelog.core.domain.policy.impl;

import java.time.Year;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaPolicy;

public class DefaultMediaPolicy implements MediaPolicy {

    private final int minYear; // 1888: "Roundhay Garden Scene"
    private final int futureSlack; // permite +1 ano (pré-cadastro de títulos)

    public DefaultMediaPolicy(int minYear, int futureSlack) {
        this.minYear = minYear;
        this.futureSlack = futureSlack;
    }

    @Override
    public void validateInvariants(Media media) {
        if (media == null) {
            throw DomainException.of(ErrorCode.INVALID_ARGUMENT, "media must not be null");
        }
        Integer year = media.getReleaseYear();
        if (year == null)
            return; // opcional

        int current = Year.now().getValue();
        int maxYear = current + futureSlack;

        if (year < minYear || year > maxYear) {
            throw DomainException.of(
                    ErrorCode.INVALID_RELEASE_YEAR,
                    "invalid release year",
                    java.util.Map.of("min", minYear, "max", maxYear, "value", year));
        }
    }
}