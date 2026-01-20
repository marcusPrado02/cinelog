package com.cine.cinelog.features.auth.web.controller;

import com.cine.cinelog.features.auth.web.dto.LoginRequest;
import com.cine.cinelog.features.auth.web.dto.LoginResponse;
import com.cine.cinelog.features.auth.web.dto.RegisterRequest;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userRepository;
    private final BusinessMetricsService metricsService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            UserJpaRepository userRepository,
            BusinessMetricsService metricsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.metricsService = metricsService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna um token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            String token = jwtTokenService.generateToken(request.email());

            // Métrica de negócio: login bem-sucedido
            metricsService.incrementLogin(true);

            return ResponseEntity.ok(new LoginResponse(token));
        } catch (BadCredentialsException e) {
            // Métricas de negócio: login falhou
            metricsService.incrementLogin(false);
            metricsService.incrementLoginFailed();

            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registra um novo usuário e retorna um token JWT")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {

        // Criar a entidade com todos os campos necessários
        UserEntity entity = new UserEntity();
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRole("USER");
        entity.setEnabled(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVersion(0L);

        // Salvar a entidade
        UserEntity saved = userRepository.save(entity);

        // Gera token para já logar o usuário
        String token = jwtTokenService.generateToken(saved.getEmail());

        // Métrica de negócio: novo usuário registrado
        metricsService.incrementUserRegistered();

        return ResponseEntity.ok(new LoginResponse(token));
    }
}
