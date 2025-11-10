package com.cine.cinelog.features.episodes.web.controller;

import com.cine.cinelog.core.application.ports.in.episodes.*;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.web.dto.EpisodeCreateRequest;
import com.cine.cinelog.features.episodes.web.dto.EpisodeResponse;
import com.cine.cinelog.features.episodes.web.dto.EpisodeUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Episodes", description = "CRUD de episódios")
@Validated
@RestController
@RequestMapping("/api/episodes")
public class EpisodeController {

    private final CreateEpisodeUseCase createUC;
    private final UpdateEpisodeUseCase updateUC;
    private final GetEpisodeUseCase getUC;
    private final ListEpisodesUseCase listUC;
    private final DeleteEpisodeUseCase deleteUC;
    private final EpisodeMapper mapper;

    public EpisodeController(CreateEpisodeUseCase createUC, UpdateEpisodeUseCase updateUC,
            GetEpisodeUseCase getUC, ListEpisodesUseCase listUC,
            DeleteEpisodeUseCase deleteUC, EpisodeMapper mapper) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria um episódio")
    @PostMapping
    public ResponseEntity<EpisodeResponse> create(@Valid @RequestBody EpisodeCreateRequest req) {
        Episode created = createUC.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/episodes/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @Operation(summary = "Atualiza um episódio")
    @PutMapping("/{id}")
    public ResponseEntity<EpisodeResponse> update(@PathVariable Long id,
            @Valid @RequestBody EpisodeUpdateRequest req) {
        Episode updated = updateUC.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Busca episódio por id")
    @GetMapping("/{id}")
    public ResponseEntity<EpisodeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(getUC.execute(id)));
    }

    @Operation(summary = "Lista episódios")
    @GetMapping
    public ResponseEntity<List<EpisodeResponse>> list() {
        return ResponseEntity.ok(listUC.execute().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Operation(summary = "Remove um episódio")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUC.execute(id);
        return ResponseEntity.noContent().build();
    }
}
