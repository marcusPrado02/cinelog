package com.cine.cinelog.features.genres.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.genre.CreateGenreUseCase;
import com.cine.cinelog.core.application.ports.in.genre.DeleteGenreUseCase;
import com.cine.cinelog.core.application.ports.in.genre.GetGenreUseCase;
import com.cine.cinelog.core.application.ports.in.genre.ListGenresUseCase;
import com.cine.cinelog.core.application.ports.in.genre.UpdateGenreUseCase;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.features.genres.mapper.GenreMapper;
import com.cine.cinelog.features.genres.web.dto.GenreCreateRequest;
import com.cine.cinelog.features.genres.web.dto.GenreResponse;
import com.cine.cinelog.features.genres.web.dto.GenreUpdateRequest;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.web.dto.PageResponse;

import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GenreControllerTest {

    private CreateGenreUseCase createUC;
    private UpdateGenreUseCase updateUC;
    private GetGenreUseCase getUC;
    private ListGenresUseCase listUC;
    private DeleteGenreUseCase deleteUC;
    private GenreMapper mapper;

    private GenreController controller;

    private BusinessMetricsService metricsService;

    @BeforeEach
    void setUp() {
        createUC = mock(CreateGenreUseCase.class);
        updateUC = mock(UpdateGenreUseCase.class);
        getUC = mock(GetGenreUseCase.class);
        listUC = mock(ListGenresUseCase.class);
        deleteUC = mock(DeleteGenreUseCase.class);
        mapper = mock(GenreMapper.class);
        metricsService = mock(BusinessMetricsService.class);

        controller = new GenreController(createUC, updateUC, getUC, listUC, deleteUC, mapper, metricsService);
    }

    @Test
    void create_shouldReturnCreatedResponse_withLocationAndBody() {
        GenreCreateRequest req = mock(GenreCreateRequest.class);
        Genre domain = mock(Genre.class);
        GenreResponse resp = mock(GenreResponse.class);

        when(req.name()).thenReturn("Drama");
        when(mapper.toDomain(eq(req))).thenReturn(domain);
        when(createUC.execute(eq(domain))).thenReturn(domain);
        when(domain.getId()).thenReturn(1L);
        when(mapper.toResponse(eq(domain))).thenReturn(resp);

        ResponseEntity<GenreResponse> response = controller.create(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(resp, response.getBody());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/api/genres/1"));

        verify(mapper).toDomain(eq(req));
        verify(createUC).execute(eq(domain));
        verify(mapper).toResponse(eq(domain));
    }

    @Test
    void update_shouldReturnOk_withUpdatedBody() {
        Long id = 42L;
        GenreUpdateRequest req = mock(GenreUpdateRequest.class);
        Genre domain = mock(Genre.class);
        GenreResponse resp = mock(GenreResponse.class);

        when(req.name()).thenReturn("Comedy");
        when(mapper.toDomain(eq(req))).thenReturn(domain);
        when(updateUC.execute(eq(id), eq(domain))).thenReturn(domain);
        when(mapper.toResponse(eq(domain))).thenReturn(resp);

        ResponseEntity<GenreResponse> response = controller.update(id, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resp, response.getBody());

        verify(mapper).toDomain(eq(req));
        verify(updateUC).execute(eq(id), eq(domain));
        verify(mapper).toResponse(eq(domain));
    }

    @Test
    void getById_shouldReturnOk_withMappedBody() {
        Long id = 7L;
        Genre domain = mock(Genre.class);
        GenreResponse resp = mock(GenreResponse.class);

        when(getUC.execute(eq(id))).thenReturn(domain);
        when(mapper.toResponse(eq(domain))).thenReturn(resp);

        ResponseEntity<GenreResponse> response = controller.getById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resp, response.getBody());

        verify(getUC).execute(eq(id));
        verify(mapper).toResponse(eq(domain));
    }

    @Test
    void list_shouldReturnOk_withMappedList() {
        Genre domain1 = mock(Genre.class);
        Genre domain2 = mock(Genre.class);
        GenreResponse resp1 = mock(GenreResponse.class);
        GenreResponse resp2 = mock(GenreResponse.class);

        List<Genre> domains = Arrays.asList(domain1, domain2);
        PageResult<Genre> pageResult = mock(PageResult.class);
        Pageable pageable = mock(Pageable.class);
        when(pageable.getSort()).thenReturn(org.springframework.data.domain.Sort.unsorted());
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(20);
        when(pageResult.content()).thenReturn(domains);
        when(listUC.execute(any(PageQuery.class))).thenReturn(pageResult);
        when(mapper.toResponse(eq(domain1))).thenReturn(resp1);
        when(mapper.toResponse(eq(domain2))).thenReturn(resp2);

        ResponseEntity<PageResponse<GenreResponse>> response = controller.list(pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PageResponse<GenreResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.content().size());
        assertEquals(Arrays.asList(resp1, resp2), body.content());

        verify(listUC).execute(any(PageQuery.class));
        verify(mapper).toResponse(eq(domain1));
        verify(mapper).toResponse(eq(domain2));
    }

    @Test
    void delete_shouldInvokeUseCase_andReturnNoContent() {
        Long id = 99L;

        // no need to stub deleteUC.execute since it returns void
        ResponseEntity<Void> response = controller.delete(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(deleteUC).execute(eq(id));
    }
}
