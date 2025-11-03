package com.cine.cinelog.features.watchentry.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(name = "WatchEntryResponse")
public record WatchEntryResponse(
        Long id,
        Long userId,
        Long mediaId,
        Long episodeId,
        Integer rating,
        String comment,
        LocalDate watchedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}