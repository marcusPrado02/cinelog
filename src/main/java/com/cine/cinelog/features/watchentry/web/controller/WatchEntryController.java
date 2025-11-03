package com.cine.cinelog.features.watchentry.web.controller;

import com.cine.cinelog.core.application.usecase.watchentry.CreateWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.DeleteWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.ListWatchEntriesService;
import com.cine.cinelog.core.application.usecase.watchentry.UpdateWatchEntryService;
import com.cine.cinelog.features.watchentry.mapper.WatchEntryMapper;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryCreateRequest;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Watch Entries", description = "Histórico de assistidos e avaliações")
@RestController
@RequestMapping("/api/watch")
public class WatchEntryController {

    private final CreateWatchEntryService createUC;
    private final ListWatchEntriesService listUC;
    private final UpdateWatchEntryService updateUC;
    private final DeleteWatchEntryService deleteUC;
    private final WatchEntryMapper WatchEntryDtoMapper;

    public WatchEntryController(CreateWatchEntryService createUC, ListWatchEntriesService listUC,
            UpdateWatchEntryService updateUC, DeleteWatchEntryService deleteUC,
            WatchEntryMapper WatchEntryDtoMapper) {
        this.createUC = createUC;
        this.listUC = listUC;
        this.updateUC = updateUC;
        this.deleteUC = deleteUC;
        this.WatchEntryDtoMapper = WatchEntryDtoMapper;
    }

    @Operation(summary = "Cria um registro de watch")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchEntryResponse create(@Valid @RequestBody WatchEntryCreateRequest req) {
        var saved = createUC.execute(WatchEntryDtoMapper.toDomain(req));
        return WatchEntryDtoMapper.toResponse(saved);
    }

    @Operation(summary = "Lista registros do usuário (paginado)")
    @GetMapping
    public Page<WatchEntryResponse> list(
            @RequestParam Long userId,
            @RequestParam(required = false) Long mediaId,
            @RequestParam(required = false) Long episodeId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = listUC.execute(userId, mediaId, episodeId, minRating, from, to, PageRequest.of(page, size));
        return result.map(WatchEntryDtoMapper::toResponse);
    }

    @Operation(summary = "Atualiza rating/comentário/metadata")
    @PutMapping("/{id}")
    public WatchEntryResponse update(@PathVariable Long id, @Valid @RequestBody WatchEntryCreateRequest req) {
        var updated = updateUC.execute(id, WatchEntryDtoMapper.toDomain(req));
        return WatchEntryDtoMapper.toResponse(updated);
    }

    @Operation(summary = "Remove um registro")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUC.execute(id);
    }
}