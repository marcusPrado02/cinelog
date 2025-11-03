package com.cine.cinelog.features.episodes.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EpisodeUpdateRequest(
        @NotNull @Min(1) Integer episodeNumber,
        @Size(max = 200) String name,
        LocalDate airDate) {
}