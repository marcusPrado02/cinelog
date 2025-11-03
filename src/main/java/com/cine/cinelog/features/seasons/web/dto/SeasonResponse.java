package com.cine.cinelog.features.seasons.web.dto;

import java.time.LocalDate;

public record SeasonResponse(Long id, Long mediaId, Integer seasonNumber, String name, LocalDate airDate) {
}