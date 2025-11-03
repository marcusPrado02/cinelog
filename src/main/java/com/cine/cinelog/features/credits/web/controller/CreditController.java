package com.cine.cinelog.features.credits.web.controller;

import com.cine.cinelog.core.application.ports.in.credits.*;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.web.dto.CreditCreateRequest;
import com.cine.cinelog.features.credits.web.dto.CreditResponse;
import com.cine.cinelog.features.credits.web.dto.CreditUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Credits", description = "CRUD de créditos")
@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreateCreditUseCase createUC;
    private final UpdateCreditUseCase updateUC;
    private final GetCreditUseCase getUC;
    private final ListCreditsUseCase listUC;
    private final DeleteCreditUseCase deleteUC;
    private final CreditMapper mapper;

    public CreditController(CreateCreditUseCase createUC, UpdateCreditUseCase updateUC,
            GetCreditUseCase getUC, ListCreditsUseCase listUC,
            DeleteCreditUseCase deleteUC, CreditMapper mapper) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria um crédito")
    @PostMapping
    public ResponseEntity<CreditResponse> create(@Valid @RequestBody CreditCreateRequest req) {
        Credit created = createUC.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/credits/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @Operation(summary = "Atualiza um crédito")
    @PutMapping("/{id}")
    public ResponseEntity<CreditResponse> update(@PathVariable Long id,
            @Valid @RequestBody CreditUpdateRequest req) {
        Credit updated = updateUC.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Busca crédito por id")
    @GetMapping("/{id}")
    public ResponseEntity<CreditResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(getUC.execute(id)));
    }

    @Operation(summary = "Lista créditos")
    @GetMapping
    public ResponseEntity<List<CreditResponse>> list() {
        return ResponseEntity.ok(listUC.execute().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Operation(summary = "Remove um crédito")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUC.execute(id);
        return ResponseEntity.noContent().build();
    }
}
