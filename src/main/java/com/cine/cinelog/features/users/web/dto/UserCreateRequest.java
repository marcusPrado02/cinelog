package com.cine.cinelog.features.users.web.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
                @NotBlank @Size(max = 120) String name,
                @NotBlank @Email @Size(max = 255) String email) {
}
