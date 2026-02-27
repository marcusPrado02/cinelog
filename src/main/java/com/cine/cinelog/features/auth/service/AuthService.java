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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A07:2025 — Serviço de autenticação centralizado.
 *
 * <p>
 * Integra todas as proteções de A07:2025:
 * </p>
 * <ul>
 * <li>Account lockout via {@link LoginAttemptService}</li>
 * <li>Anti-enumeração via {@link AntiEnumerationService}</li>
 * <li>Política de senha via {@link PasswordPolicyValidator}</li>
 * <li>Refresh token rotation via {@link RefreshTokenService}</li>
 * <li>Métricas de negócio via {@link BusinessMetricsService}</li>
 * </ul>
 */
@Service
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userRepository;
    private final BusinessMetricsService metricsService;
    private final AntiEnumerationService antiEnumerationService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final RefreshTokenService refreshTokenService;
    private final long accessTokenExpirationSeconds;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            UserJpaRepository userRepository,
            BusinessMetricsService metricsService,
            AntiEnumerationService antiEnumerationService,
            LoginAttemptService loginAttemptService,
            PasswordPolicyValidator passwordPolicyValidator,
            RefreshTokenService refreshTokenService,
            @Value("${cinelog.security.jwt.expiration-seconds:3600}") long accessTokenExpirationSeconds) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.metricsService = metricsService;
        this.antiEnumerationService = antiEnumerationService;
        this.loginAttemptService = loginAttemptService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    /**
     * Realiza login com proteções de A07:2025.
     *
     * @throws LockedException         se a conta está bloqueada por excesso de
     *                                 tentativas
     * @throws BadCredentialsException se email/senha estão incorretos
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
        String email = request.email();

        // A07: Verificar bloqueio por email E por IP
        if (loginAttemptService.isBlocked(email)) {
            long retryAfter = loginAttemptService.getSecondsUntilUnlock(email);
            antiEnumerationService.addTimingNoise();
            log.warn("A07:2025 — Login bloqueado por excesso de tentativas: email_hash={}",
                    maskEmail(email));
            throw new AccountLockedException(retryAfter);
        }

        if (loginAttemptService.isBlocked(clientIp)) {
            long retryAfter = loginAttemptService.getSecondsUntilUnlock(clientIp);
            antiEnumerationService.addTimingNoise();
            log.warn("A07:2025 — Login bloqueado por IP: ip={}", clientIp);
            throw new AccountLockedException(retryAfter);
        }

        try {
            // Autenticar via Spring Security (BCrypt factor 12)
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));

            // Login ok — limpar contadores de falha
            loginAttemptService.recordSuccessfulLogin(email);
            loginAttemptService.recordSuccessfulLogin(clientIp);
            antiEnumerationService.logAuthAttempt(email, true);
            metricsService.incrementLogin(true);

            // Gerar tokens
            CinelogUserDetails userDetails = (CinelogUserDetails) auth.getPrincipal();
            String accessToken = jwtTokenService.generateToken(email);
            String refreshToken = refreshTokenService.createRefreshToken(
                    userDetails.getUserId(), email, clientIp, userAgent);

            return new AuthResponse(accessToken, refreshToken, accessTokenExpirationSeconds);

        } catch (BadCredentialsException e) {
            // Registrar falha para email E IP
            loginAttemptService.recordFailedAttempt(email);
            loginAttemptService.recordFailedAttempt(clientIp);
            antiEnumerationService.logAuthAttempt(email, false);
            antiEnumerationService.addTimingNoise();

            metricsService.incrementLogin(false);
            metricsService.incrementLoginFailed();

            // A06: Mensagem genérica (anti-enumeração)
            throw new BadCredentialsException(AntiEnumerationService.GENERIC_AUTH_ERROR);
        }
    }

    /**
     * Registra novo usuário com validação de política de senha.
     *
     * @throws PasswordPolicyException se a senha não atende à política
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp, String userAgent) {
        // A07: Validar política de senha
        List<String> violations = passwordPolicyValidator.validate(request.password(), request.email());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(violations);
        }

        // A06: Verificar email duplicado sem revelar existência (anti-enumeração)
        if (userRepository.existsByEmail(request.email())) {
            // NÃO retornar "email já existe" — isso permite enumeração
            antiEnumerationService.addTimingNoise();
            log.warn("A07:2025 — Tentativa de registro com email existente");
            // Retornar sucesso falso com mensagem genérica (anti-enumeração)
            throw new EmailAlreadyExistsException();
        }

        // Criar usuário
        UserEntity entity = new UserEntity();
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRole("USER");
        entity.setEnabled(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVersion(0L);
        UserEntity saved = userRepository.save(entity);

        metricsService.incrementUserRegistered();

        // Gerar tokens
        String accessToken = jwtTokenService.generateToken(saved.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(
                saved.getId(), saved.getEmail(), clientIp, userAgent);

        return new AuthResponse(accessToken, refreshToken, accessTokenExpirationSeconds);
    }

    /**
     * Renova tokens via refresh token rotation.
     */
    @Transactional
    public AuthResponse refreshTokens(String currentRefreshToken, String clientIp, String userAgent) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken(currentRefreshToken,
                clientIp, userAgent);

        String accessToken = jwtTokenService.generateToken(result.userEmail());

        return new AuthResponse(accessToken, result.newRefreshToken(), accessTokenExpirationSeconds);
    }

    /**
     * Logout — revoga todos os refresh tokens do usuário.
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Logout completo: userId={}", userId);
    }

    private String maskEmail(String email) {
        if (email == null || email.length() <= 4)
            return "****";
        return email.substring(0, 2) + "***" + email.substring(email.length() - 2);
    }

    // ── Exceções específicas de autenticação ──

    /**
     * Conta bloqueada por excesso de tentativas de login.
     */
    public static class AccountLockedException extends RuntimeException {
        private final long retryAfterSeconds;

        public AccountLockedException(long retryAfterSeconds) {
            super("Conta temporariamente bloqueada por excesso de tentativas.");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    /**
     * Senha não atende à política de complexidade.
     */
    public static class PasswordPolicyException extends RuntimeException {
        private final List<String> violations;

        public PasswordPolicyException(List<String> violations) {
            super("Senha não atende à política de segurança.");
            this.violations = violations;
        }

        public List<String> getViolations() {
            return violations;
        }
    }

    /**
     * Email já cadastrado (tratado com anti-enumeração).
     */
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException() {
            super("Erro ao processar registro.");
        }
    }
}
