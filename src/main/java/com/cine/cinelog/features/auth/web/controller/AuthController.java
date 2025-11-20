package com.cine.cinelog.features.auth.web.controller;

import com.cine.cinelog.core.application.ports.in.user.CreateUserUseCase;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.auth.web.dto.LoginRequest;
import com.cine.cinelog.features.auth.web.dto.LoginResponse;
import com.cine.cinelog.features.auth.web.dto.RegisterRequest;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
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

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final CreateUserUseCase createUserUseCase;
    private final UserJpaRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            CreateUserUseCase createUserUseCase,
            UserJpaRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.createUserUseCase = createUserUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna um token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            String token = jwtTokenService.generateToken(request.email());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registra um novo usuário e retorna um token JWT")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {

        // 1) Cria usuário via caso de uso (sem senha)
        User domainUser = new User();
        domainUser.setName(request.name());
        domainUser.setEmail(request.email());
        domainUser.setCreatedAt(OffsetDateTime.now());

        User created = createUserUseCase.execute(domainUser);

        // 2) Atualiza entidade com senha + role
        UserEntity entity = userRepository.findById(created.getId())
                .orElseThrow(); // em teoria não deve acontecer

        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRole("USER");
        entity.setEnabled(true);
        userRepository.save(entity);

        // 3) Gera token para já logar o cara
        String token = jwtTokenService.generateToken(entity.getEmail());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}