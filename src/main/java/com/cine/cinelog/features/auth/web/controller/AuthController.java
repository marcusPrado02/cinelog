package com.cine.cinelog.features.auth.web.controller;

import com.cine.cinelog.features.auth.service.AuthService;
import com.cine.cinelog.features.auth.web.dto.AuthResponse;
import com.cine.cinelog.features.auth.web.dto.LoginRequest;
import com.cine.cinelog.features.auth.web.dto.RefreshTokenRequest;
import com.cine.cinelog.features.auth.web.dto.RegisterRequest;
import com.cine.cinelog.shared.security.CinelogUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * A07:2025 — Controller de autenticação (thin layer).
 *
 * <p>
 * Delega toda lógica ao {@link AuthService}, responsável por integrar
 * lockout, anti-enumeração, política de senha e refresh token rotation.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna access + refresh tokens")
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @ApiResponse(responseCode = "423", description = "Conta bloqueada por excesso de tentativas")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Registra novo usuário e retorna access + refresh tokens")
    @ApiResponse(responseCode = "200", description = "Registro bem-sucedido")
    @ApiResponse(responseCode = "400", description = "Senha não atende à política de segurança")
    @ApiResponse(responseCode = "409", description = "Erro no processamento do registro")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.register(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova tokens via refresh token rotation")
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou reutilizado")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.refreshTokens(
                request.refreshToken(), clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoga todos os refresh tokens do usuário")
    @ApiResponse(responseCode = "204", description = "Logout realizado")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CinelogUserDetails user) {
        if (user == null) {
            // Keycloak/external JWT — sem refresh tokens locais para revogar
            return ResponseEntity.noContent().build();
        }
        authService.logout(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Extrai IP do cliente considerando proxy reverso (X-Forwarded-For).
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Pega apenas o primeiro IP (cliente real)
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
