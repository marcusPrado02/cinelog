package com.cine.cinelog.features.users.web.dto;

import java.time.OffsetDateTime;

public record UserResponse(
                Long id, String name, String email, OffsetDateTime createdAt) {
}