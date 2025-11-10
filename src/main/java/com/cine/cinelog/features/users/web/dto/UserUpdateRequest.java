package com.cine.cinelog.features.users.web.dto;

import jakarta.validation.constraints.*;

public record UserUpdateRequest(
                @NotBlank @Size(max = 120) String name) {
}