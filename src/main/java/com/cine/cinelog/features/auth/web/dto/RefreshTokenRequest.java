package com.cine.cinelog.features.auth.web.dto;

/**
 * A07:2025 — Request para renovação de tokens via refresh token.
 */
public record RefreshTokenRequest(String refreshToken) {
}
