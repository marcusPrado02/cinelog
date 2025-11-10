package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;
import jakarta.validation.constraints.*;

public record MediaUpdateRequest(
                @NotBlank @Size(max = 300) String title,
                @NotNull MediaType type,
                @Min(1800) @Max(2100) Integer releaseYear,
                @Size(max = 300) String originalTitle,
                @Size(max = 10) String originalLanguage,
                @Size(max = 300) String posterUrl,
                @Size(max = 300) String backdropUrl,
                String overview) {
}
