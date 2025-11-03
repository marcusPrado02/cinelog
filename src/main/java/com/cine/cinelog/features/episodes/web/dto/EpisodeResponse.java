package com.cine.cinelog.features.episodes.web.dto;

import java.time.LocalDate;

public record EpisodeResponse(Long id, Long seasonId, Integer episodeNumber, String name, LocalDate airDate) {
}