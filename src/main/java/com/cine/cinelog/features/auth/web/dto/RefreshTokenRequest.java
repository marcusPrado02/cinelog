package com.cine.cinelog.features.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A07:2025 — Request para renovação de tokens via refresh token.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken é obrigatório") String refreshToken) {
}
