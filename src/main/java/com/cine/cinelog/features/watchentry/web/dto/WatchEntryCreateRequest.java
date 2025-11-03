package com.cine.cinelog.features.watchentry.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(name = "WatchEntryCreateRequest")
public record WatchEntryCreateRequest(
        @NotNull Long userId,
        Long mediaId,
        Long episodeId,
        @Min(0) @Max(10) Integer rating,
        @Size(max = 10_000) String comment,
        LocalDate watchedAt) {
}
