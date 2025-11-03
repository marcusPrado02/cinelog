package com.cine.cinelog.features.seasons.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SeasonUpdateRequest(
        @NotNull @Min(0) Integer seasonNumber,
        @Size(max = 200) String name,
        LocalDate airDate) {
}