package com.cine.cinelog.features.auth.web.dto;

/**
 * A07:2025 — Resposta de autenticação com access + refresh tokens.
 *
 * @param accessToken  JWT de curta duração (1h) para autenticação de requests
 * @param refreshToken token opaco de longa duração (7d) para renovar o
 *                     accessToken
 * @param tokenType    sempre "Bearer"
 * @param expiresIn    duração do accessToken em segundos
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public AuthResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
