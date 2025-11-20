package com.cine.cinelog.features.auth.web.dto;

public record LoginResponse(
        String accessToken,
        String tokenType) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
