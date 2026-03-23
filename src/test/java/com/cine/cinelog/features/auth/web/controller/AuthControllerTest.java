package com.cine.cinelog.features.auth.web.controller;

import com.cine.cinelog.features.auth.service.AuthService;
import com.cine.cinelog.features.auth.web.dto.AuthResponse;
import com.cine.cinelog.features.auth.web.dto.LoginRequest;
import com.cine.cinelog.features.auth.web.dto.RefreshTokenRequest;
import com.cine.cinelog.features.auth.web.dto.RegisterRequest;
import com.cine.cinelog.shared.security.CinelogUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");
        httpRequest.addHeader("User-Agent", "TestClient/1.0");
    }

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse("access-token-123", "refresh-token-456", 3600);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should return 200 with tokens on valid credentials")
        void shouldReturnTokensOnValidLogin() {
            LoginRequest request = new LoginRequest("user@cinelog.dev", "Str0ngP@ss!");
            when(authService.login(eq(request), eq("127.0.0.1"), eq("TestClient/1.0")))
                    .thenReturn(sampleAuthResponse());

            ResponseEntity<AuthResponse> response = authController.login(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isEqualTo("access-token-123");
            assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().expiresIn()).isEqualTo(3600);
        }

        @Test
        @DisplayName("should extract client IP from X-Forwarded-For header")
        void shouldExtractIpFromXForwardedFor() {
            httpRequest.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
            LoginRequest request = new LoginRequest("user@cinelog.dev", "Str0ngP@ss!");
            when(authService.login(eq(request), eq("203.0.113.50"), any()))
                    .thenReturn(sampleAuthResponse());

            authController.login(request, httpRequest);

            verify(authService).login(eq(request), eq("203.0.113.50"), eq("TestClient/1.0"));
        }

        @Test
        @DisplayName("should use remoteAddr when X-Forwarded-For is absent")
        void shouldUseRemoteAddrWhenNoForwardedFor() {
            LoginRequest request = new LoginRequest("user@cinelog.dev", "Str0ngP@ss!");
            when(authService.login(any(), any(), any())).thenReturn(sampleAuthResponse());

            authController.login(request, httpRequest);

            verify(authService).login(eq(request), eq("127.0.0.1"), eq("TestClient/1.0"));
        }

        @Test
        @DisplayName("should propagate AuthService exceptions to caller")
        void shouldPropagateExceptions() {
            LoginRequest request = new LoginRequest("user@cinelog.dev", "wrong");
            when(authService.login(any(), any(), any()))
                    .thenThrow(new RuntimeException("Bad credentials"));

            assertThatThrownBy(() -> authController.login(request, httpRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Bad credentials");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should return 200 with tokens on valid registration")
        void shouldReturnTokensOnValidRegistration() {
            RegisterRequest request = new RegisterRequest("Alice", "alice@cinelog.dev", "Str0ngP@ss!");
            when(authService.register(eq(request), eq("127.0.0.1"), eq("TestClient/1.0")))
                    .thenReturn(sampleAuthResponse());

            ResponseEntity<AuthResponse> response = authController.register(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            verify(authService).register(eq(request), eq("127.0.0.1"), eq("TestClient/1.0"));
        }

        @Test
        @DisplayName("should extract client IP from X-Forwarded-For on register")
        void shouldExtractIpOnRegister() {
            httpRequest.addHeader("X-Forwarded-For", "10.0.0.1");
            RegisterRequest request = new RegisterRequest("Bob", "bob@cinelog.dev", "Str0ngP@ss!");
            when(authService.register(any(), any(), any())).thenReturn(sampleAuthResponse());

            authController.register(request, httpRequest);

            verify(authService).register(eq(request), eq("10.0.0.1"), eq("TestClient/1.0"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh Token
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("should return new tokens on valid refresh")
        void shouldReturnNewTokensOnValidRefresh() {
            RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
            AuthResponse newTokens = new AuthResponse("new-access", "new-refresh", 3600);
            when(authService.refreshTokens(eq("old-refresh-token"), eq("127.0.0.1"), eq("TestClient/1.0")))
                    .thenReturn(newTokens);

            ResponseEntity<AuthResponse> response = authController.refreshToken(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().accessToken()).isEqualTo("new-access");
            assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh");
        }

        @Test
        @DisplayName("should propagate exception on invalid refresh token")
        void shouldPropagateOnInvalidRefreshToken() {
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");
            when(authService.refreshTokens(any(), any(), any()))
                    .thenThrow(new RuntimeException("Refresh token revoked"));

            assertThatThrownBy(() -> authController.refreshToken(request, httpRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("revoked");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("should return 204 and revoke tokens for authenticated user")
        void shouldReturn204AndRevokeTokens() {
            CinelogUserDetails user = mock(CinelogUserDetails.class);
            when(user.getUserId()).thenReturn(42L);

            ResponseEntity<Void> response = authController.logout(user);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService).logout(42L);
        }

        @Test
        @DisplayName("should return 204 without revoking when user is null (Keycloak JWT)")
        void shouldReturn204WithoutRevokingForKeycloakUser() {
            ResponseEntity<Void> response = authController.logout(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService, never()).logout(any());
        }
    }
}
