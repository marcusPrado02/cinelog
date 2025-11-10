package com.cine.cinelog.features.credits.web.dto;

public record CreditResponse(
                Long id, Long mediaId, Long personId, String role, String characterName, Short orderIndex) {
}