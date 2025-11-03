package com.cine.cinelog.features.seasons.web.controller;

import com.cine.cinelog.core.application.ports.in.season.*;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.web.dto.SeasonCreateRequest;
import com.cine.cinelog.features.seasons.web.dto.SeasonResponse;
import com.cine.cinelog.features.seasons.web.dto.SeasonUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Seasons")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    private final CreateSeasonUseCase createUC;
    private final UpdateSeasonUseCase updateUC;
    private final GetSeasonUseCase getUC;
    private final ListSeasonsUseCase listUC;
    private final DeleteSeasonUseCase deleteUC;
    private final SeasonMapper mapper;

    public SeasonController(CreateSeasonUseCase createUC, UpdateSeasonUseCase updateUC,
            GetSeasonUseCase getUC, ListSeasonsUseCase listUC,
            DeleteSeasonUseCase deleteUC, SeasonMapper mapper) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria uma temporada")
    @PostMapping
    public ResponseEntity<SeasonResponse> create(@Valid @RequestBody SeasonCreateRequest req) {
        Season created = createUC.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/seasons/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @Operation(summary = "Atualiza uma temporada")
    @PutMapping("/{id}")
    public ResponseEntity<SeasonResponse> update(@PathVariable Long id,
            @Valid @RequestBody SeasonUpdateRequest req) {
        Season updated = updateUC.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Busca temporada por id")
    @GetMapping("/{id}")
    public ResponseEntity<SeasonResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(getUC.execute(id)));
    }

    @Operation(summary = "Lista temporadas")
    @GetMapping
    public ResponseEntity<List<SeasonResponse>> list() {
        return ResponseEntity.ok(listUC.execute().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Operation(summary = "Remove uma temporada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUC.execute(id);
        return ResponseEntity.noContent().build();
    }
}
