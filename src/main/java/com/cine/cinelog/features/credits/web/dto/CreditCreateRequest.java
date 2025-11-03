package com.cine.cinelog.features.credits.web.dto;

import jakarta.validation.constraints.*;

public record CreditCreateRequest(
        @NotNull Long mediaId,
        @NotNull Long personId,
        @NotBlank String role,
        @Size(max = 200) String characterName,
        @Min(0) @Max(32767) Short orderIndex) {
}
