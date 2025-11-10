package com.cine.cinelog.features.media.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.features.media.web.dto.MediaUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

/**
 * Controller REST responsável por expor endpoints CRUD para a entidade
 * Media. A controller atua como camada de entrada da aplicação e faz a
 * conversão entre DTOs HTTP e casos de uso da aplicação.
 */

@Tag(name = "Media", description = "CRUD de filmes e séries")
@Validated
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final CreateMediaUseCase create;
    private final UpdateMediaUseCase update;
    private final GetMediaUseCase get;
    private final ListMediaUseCase list;
    private final DeleteMediaUseCase delete;
    private final MediaMapper mapper;

    public MediaController(CreateMediaUseCase create, UpdateMediaUseCase update,
            GetMediaUseCase get, ListMediaUseCase list,
            DeleteMediaUseCase delete, MediaMapper mapper) {
        this.create = create;
        this.update = update;
        this.get = get;
        this.list = list;
        this.delete = delete;
        this.mapper = mapper;
    }

    // Converte DTO para domínio, executa caso de uso e devolve DTO de
    // resposta com status 201 CREATED.
    @Operation(summary = "Cria uma mídia", description = "Cadastra um filme/série")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MediaResponse> create(@Valid @RequestBody MediaCreateRequest req) {
        var saved = create.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/media/" + saved.getId()))
                .body(mapper.toResponse(saved));
    }

    @Operation(summary = "Busca por id")
    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(get.execute(id)));
    }

    @Operation(summary = "Lista mídias", description = "Filtro por type e busca por título")
    @GetMapping
    public ResponseEntity<List<MediaResponse>> list(@RequestParam(required = false) MediaType type,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Lista mídias aplicando filtros e paginação, mapeando para DTO.
        var mediaList = list.execute(type, q, page, size).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(mediaList);
    }

    @Operation(summary = "Atualiza uma mídia")
    @PutMapping("/{id}")
    public ResponseEntity<MediaResponse> update(@PathVariable Long id, @Valid @RequestBody MediaUpdateRequest req) {
        var updated = update.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Remove uma mídia")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        delete.execute(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail notFound(IllegalArgumentException ex) {
        // Retorna um ProblemDetail com a mensagem da exceção para o cliente.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}