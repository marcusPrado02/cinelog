package com.cine.cinelog.features.genres.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreCreateRequest(@NotBlank @Size(max = 100) String name) {
}