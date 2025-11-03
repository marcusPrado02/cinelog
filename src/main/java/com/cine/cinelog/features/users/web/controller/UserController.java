package com.cine.cinelog.features.users.web.controller;

import com.cine.cinelog.core.application.ports.in.user.*;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.web.dto.UserCreateRequest;
import com.cine.cinelog.features.users.web.dto.UserResponse;
import com.cine.cinelog.features.users.web.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Users", description = "CRUD de usuários")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUC;
    private final UpdateUserUseCase updateUC;
    private final GetUserUseCase getUC;
    private final ListUsersUseCase listUC;
    private final DeleteUserUseCase deleteUC;
    private final UserMapper mapper;

    public UserController(CreateUserUseCase createUC, UpdateUserUseCase updateUC,
            GetUserUseCase getUC, ListUsersUseCase listUC,
            DeleteUserUseCase deleteUC, UserMapper mapper) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria um usuário")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        User created = createUC.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/users/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @Operation(summary = "Atualiza um usuário")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest req) {
        User updated = updateUC.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Busca usuário por id")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(getUC.execute(id)));
    }

    @Operation(summary = "Lista usuários")
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(listUC.execute().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Operation(summary = "Remove um usuário")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUC.execute(id);
        return ResponseEntity.noContent().build();
    }
}
