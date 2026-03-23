package com.cine.cinelog.features.auth.service;

import com.cine.cinelog.features.auth.web.dto.AuthResponse;
import com.cine.cinelog.features.auth.web.dto.LoginRequest;
import com.cine.cinelog.features.auth.web.dto.RegisterRequest;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.security.AntiEnumerationService;
import com.cine.cinelog.shared.security.CinelogUserDetails;
import com.cine.cinelog.shared.security.JwtTokenService;
import com.cine.cinelog.shared.security.LoginAttemptService;
import com.cine.cinelog.shared.security.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserJpaRepository userRepository;
    @Mock private BusinessMetricsService metricsService;
    @Mock private AntiEnumerationService antiEnumerationService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private PasswordPolicyValidator passwordPolicyValidator;
    @Mock private RefreshTokenService refreshTokenService;

    private AuthService authService;

    private static final long EXPIRATION_SECONDS = 3600;
    private static final String CLIENT_IP = "192.168.1.100";
    private static final String USER_AGENT = "TestClient/1.0";

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                jwtTokenService,
                passwordEncoder,
                userRepository,
                metricsService,
                antiEnumerationService,
                loginAttemptService,
                passwordPolicyValidator,
                refreshTokenService,
                EXPIRATION_SECONDS
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should authenticate and return tokens on valid credentials")
        void shouldAuthenticateAndReturnTokens() {
            LoginRequest request = new LoginRequest("alice@cinelog.dev", "Str0ngP@ss!");
            CinelogUserDetails userDetails = mock(CinelogUserDetails.class);
            when(userDetails.getUserId()).thenReturn(1L);
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);

            when(loginAttemptService.isBlocked("alice@cinelog.dev")).thenReturn(false);
            when(loginAttemptService.isBlocked(CLIENT_IP)).thenReturn(false);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtTokenService.generateToken("alice@cinelog.dev")).thenReturn("jwt-access-token");
            when(refreshTokenService.createRefreshToken(eq(1L), eq("alice@cinelog.dev"), eq(CLIENT_IP), eq(USER_AGENT)))
                    .thenReturn("refresh-token-abc");

            AuthResponse response = authService.login(request, CLIENT_IP, USER_AGENT);

            assertThat(response.accessToken()).isEqualTo("jwt-access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-abc");
            assertThat(response.expiresIn()).isEqualTo(EXPIRATION_SECONDS);

            verify(loginAttemptService).recordSuccessfulLogin("alice@cinelog.dev");
            verify(loginAttemptService).recordSuccessfulLogin(CLIENT_IP);
            verify(metricsService).incrementLogin(true);
        }

        @Test
        @DisplayName("should throw AccountLockedException when email is blocked")
        void shouldThrowWhenEmailBlocked() {
            LoginRequest request = new LoginRequest("blocked@cinelog.dev", "pass");
            when(loginAttemptService.isBlocked("blocked@cinelog.dev")).thenReturn(true);
            when(loginAttemptService.getSecondsUntilUnlock("blocked@cinelog.dev")).thenReturn(600L);

            assertThatThrownBy(() -> authService.login(request, CLIENT_IP, USER_AGENT))
                    .isInstanceOf(AuthService.AccountLockedException.class)
                    .satisfies(ex -> {
                        AuthService.AccountLockedException locked = (AuthService.AccountLockedException) ex;
                        assertThat(locked.getRetryAfterSeconds()).isEqualTo(600L);
                    });

            verify(antiEnumerationService).addTimingNoise();
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("should throw AccountLockedException when IP is blocked")
        void shouldThrowWhenIpBlocked() {
            LoginRequest request = new LoginRequest("user@cinelog.dev", "pass");
            when(loginAttemptService.isBlocked("user@cinelog.dev")).thenReturn(false);
            when(loginAttemptService.isBlocked(CLIENT_IP)).thenReturn(true);
            when(loginAttemptService.getSecondsUntilUnlock(CLIENT_IP)).thenReturn(300L);

            assertThatThrownBy(() -> authService.login(request, CLIENT_IP, USER_AGENT))
                    .isInstanceOf(AuthService.AccountLockedException.class);

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("should record failed attempt and throw generic error on bad credentials")
        void shouldRecordFailedAttemptOnBadCredentials() {
            LoginRequest request = new LoginRequest("user@cinelog.dev", "wrongpass");
            when(loginAttemptService.isBlocked(any())).thenReturn(false);
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request, CLIENT_IP, USER_AGENT))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage(AntiEnumerationService.GENERIC_AUTH_ERROR);

            verify(loginAttemptService).recordFailedAttempt("user@cinelog.dev");
            verify(loginAttemptService).recordFailedAttempt(CLIENT_IP);
            verify(metricsService).incrementLogin(false);
            verify(metricsService).incrementLoginFailed();
            verify(antiEnumerationService).addTimingNoise();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should create user and return tokens on valid registration")
        void shouldCreateUserAndReturnTokens() {
            RegisterRequest request = new RegisterRequest("Alice", "alice@cinelog.dev", "Str0ngP@ss!");
            when(passwordPolicyValidator.validate("Str0ngP@ss!", "alice@cinelog.dev"))
                    .thenReturn(Collections.emptyList());
            when(userRepository.existsByEmail("alice@cinelog.dev")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngP@ss!")).thenReturn("$2a$12$hashed");

            UserEntity savedEntity = new UserEntity();
            savedEntity.setId(42L);
            savedEntity.setEmail("alice@cinelog.dev");
            when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
            when(jwtTokenService.generateToken("alice@cinelog.dev")).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(eq(42L), eq("alice@cinelog.dev"), eq(CLIENT_IP), eq(USER_AGENT)))
                    .thenReturn("refresh-token");

            AuthResponse response = authService.register(request, CLIENT_IP, USER_AGENT);

            assertThat(response.accessToken()).isEqualTo("jwt-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            UserEntity captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("Alice");
            assertThat(captured.getEmail()).isEqualTo("alice@cinelog.dev");
            assertThat(captured.getPasswordHash()).isEqualTo("$2a$12$hashed");
            assertThat(captured.getRole()).isEqualTo("USER");
            assertThat(captured.isEnabled()).isTrue();

            verify(metricsService).incrementUserRegistered();
        }

        @Test
        @DisplayName("should throw PasswordPolicyException when password is weak")
        void shouldThrowOnWeakPassword() {
            RegisterRequest request = new RegisterRequest("Bob", "bob@cinelog.dev", "123");
            when(passwordPolicyValidator.validate("123", "bob@cinelog.dev"))
                    .thenReturn(List.of("Must be at least 8 characters", "Must contain uppercase"));

            assertThatThrownBy(() -> authService.register(request, CLIENT_IP, USER_AGENT))
                    .isInstanceOf(AuthService.PasswordPolicyException.class)
                    .satisfies(ex -> {
                        AuthService.PasswordPolicyException pex = (AuthService.PasswordPolicyException) ex;
                        assertThat(pex.getViolations()).hasSize(2);
                    });

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw EmailAlreadyExistsException on duplicate email")
        void shouldThrowOnDuplicateEmail() {
            RegisterRequest request = new RegisterRequest("Alice", "alice@cinelog.dev", "Str0ngP@ss!");
            when(passwordPolicyValidator.validate(any(), any())).thenReturn(Collections.emptyList());
            when(userRepository.existsByEmail("alice@cinelog.dev")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request, CLIENT_IP, USER_AGENT))
                    .isInstanceOf(AuthService.EmailAlreadyExistsException.class);

            verify(antiEnumerationService).addTimingNoise();
            verify(userRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh Tokens
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshTokens")
    class RefreshTokenTests {

        @Test
        @DisplayName("should rotate refresh token and return new token pair")
        void shouldRotateAndReturnNewTokens() {
            RefreshTokenService.RotationResult rotationResult =
                    new RefreshTokenService.RotationResult("new-refresh-token", 1L, "user@cinelog.dev");
            when(refreshTokenService.rotateRefreshToken("old-token", CLIENT_IP, USER_AGENT))
                    .thenReturn(rotationResult);
            when(jwtTokenService.generateToken("user@cinelog.dev")).thenReturn("new-access-token");

            AuthResponse response = authService.refreshTokens("old-token", CLIENT_IP, USER_AGENT);

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.expiresIn()).isEqualTo(EXPIRATION_SECONDS);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("should revoke all user refresh tokens")
        void shouldRevokeAllTokens() {
            authService.logout(42L);

            verify(refreshTokenService).revokeAllUserTokens(42L);
        }
    }
}
