package com.cine.cinelog.features.genres.web.controller;

import com.cine.cinelog.core.application.ports.in.genre.*;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.features.genres.mapper.GenreMapper;
import com.cine.cinelog.features.genres.web.dto.GenreCreateRequest;
import com.cine.cinelog.features.genres.web.dto.GenreResponse;
import com.cine.cinelog.features.genres.web.dto.GenreUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Genres", description = "CRUD de gêneros")
@Validated
@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final CreateGenreUseCase createUC;
    private final UpdateGenreUseCase updateUC;
    private final GetGenreUseCase getUC;
    private final ListGenresUseCase listUC;
    private final DeleteGenreUseCase deleteUC;
    private final GenreMapper mapper;

    public GenreController(CreateGenreUseCase createUC, UpdateGenreUseCase updateUC,
            GetGenreUseCase getUC, ListGenresUseCase listUC,
            DeleteGenreUseCase deleteUC, GenreMapper mapper) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria um gênero")
    @PostMapping
    public ResponseEntity<GenreResponse> create(@Valid @RequestBody GenreCreateRequest req) {
        Genre created = createUC.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/genres/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @Operation(summary = "Atualiza um gênero")
    @PutMapping("/{id}")
    public ResponseEntity<GenreResponse> update(@PathVariable Long id,
            @Valid @RequestBody GenreUpdateRequest req) {
        Genre updated = updateUC.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Busca um gênero por id")
    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(getUC.execute(id)));
    }

    @Operation(summary = "Lista gêneros")
    @GetMapping
    public ResponseEntity<List<GenreResponse>> list() {
        return ResponseEntity.ok(listUC.execute().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Operation(summary = "Remove um gênero")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUC.execute(id);
        return ResponseEntity.noContent().build();
    }
}
