package com.cine.cinelog.features.people.web.controller;

import com.cine.cinelog.core.application.ports.in.person.*;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.web.dto.PersonCreateRequest;
import com.cine.cinelog.features.people.web.dto.PersonResponse;
import com.cine.cinelog.features.people.web.dto.PersonUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "People", description = "CRUD de pessoas")
@Validated
@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final CreatePersonUseCase createUC;
    private final UpdatePersonUseCase updateUC;
    private final GetPersonUseCase getUC;
    private final ListPeopleUseCase listUC;
    private final DeletePersonUseCase deleteUC;
    private final PersonMapper mapper;

    public PersonController(CreatePersonUseCase createUC, UpdatePersonUseCase updateUC,
            GetPersonUseCase getUC, ListPeopleUseCase listUC,
            DeletePersonUseCase deleteUC, PersonMapper mapper) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria uma pessoa")
    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonCreateRequest req) {
        Person created = createUC.execute(mapper.toDomain(req));
        return ResponseEntity.created(URI.create("/api/people/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @Operation(summary = "Atualiza uma pessoa")
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(@PathVariable Long id,
            @Valid @RequestBody PersonUpdateRequest req) {
        Person updated = updateUC.execute(id, mapper.toDomain(req));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Busca pessoa por id")
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(getUC.execute(id)));
    }

    @Operation(summary = "Lista pessoas")
    @GetMapping
    public ResponseEntity<List<PersonResponse>> list() {
        return ResponseEntity.ok(listUC.execute().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Operation(summary = "Remove uma pessoa")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUC.execute(id);
        return ResponseEntity.noContent().build();
    }
}
