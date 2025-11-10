package com.cine.cinelog.features.seasons.web.controller;

import com.cine.cinelog.core.application.ports.in.season.CreateSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.season.DeleteSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.season.GetSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.season.ListSeasonsUseCase;
import com.cine.cinelog.core.application.ports.in.season.UpdateSeasonUseCase;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.web.dto.SeasonCreateRequest;
import com.cine.cinelog.features.seasons.web.dto.SeasonResponse;
import com.cine.cinelog.features.seasons.web.dto.SeasonUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeasonControllerTest {

    private CreateSeasonUseCase createUC;
    private UpdateSeasonUseCase updateUC;
    private GetSeasonUseCase getUC;
    private ListSeasonsUseCase listUC;
    private DeleteSeasonUseCase deleteUC;
    private SeasonMapper mapper;

    private SeasonController controller;

    @BeforeEach
    void setUp() {
        createUC = mock(CreateSeasonUseCase.class);
        updateUC = mock(UpdateSeasonUseCase.class);
        getUC = mock(GetSeasonUseCase.class);
        listUC = mock(ListSeasonsUseCase.class);
        deleteUC = mock(DeleteSeasonUseCase.class);
        mapper = mock(SeasonMapper.class);

        controller = new SeasonController(createUC, updateUC, getUC, listUC, deleteUC, mapper);
    }

    @Test
    void createShouldReturnCreatedWithLocationAndBody() {
        SeasonCreateRequest req = mock(SeasonCreateRequest.class);
        Season domain = mock(Season.class);
        SeasonResponse resp = mock(SeasonResponse.class);

        when(mapper.toDomain(any(SeasonCreateRequest.class))).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(domain);
        when(domain.getId()).thenReturn(123L);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<SeasonResponse> result = controller.create(req);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals(URI.create("/api/seasons/123"), result.getHeaders().getLocation());
        assertSame(resp, result.getBody());
    }

    @Test
    void updateShouldReturnOkWithBody() {
        Long id = 5L;
        SeasonUpdateRequest req = mock(SeasonUpdateRequest.class);
        Season domainFromReq = mock(Season.class);
        Season updatedDomain = mock(Season.class);
        SeasonResponse resp = mock(SeasonResponse.class);

        when(mapper.toDomain(any(SeasonUpdateRequest.class))).thenReturn(domainFromReq);
        when(updateUC.execute(eq(id), any(Season.class))).thenReturn(updatedDomain);
        when(mapper.toResponse(updatedDomain)).thenReturn(resp);

        ResponseEntity<SeasonResponse> result = controller.update(id, req);

        assertEquals(200, result.getStatusCodeValue());
        assertSame(resp, result.getBody());
    }

    @Test
    void getByIdShouldReturnOkWithBody() {
        Long id = 10L;
        Season domain = mock(Season.class);
        SeasonResponse resp = mock(SeasonResponse.class);

        when(getUC.execute(eq(id))).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<SeasonResponse> result = controller.getById(id);

        assertEquals(200, result.getStatusCodeValue());
        assertSame(resp, result.getBody());
    }

    @Test
    void listShouldReturnOkWithMappedList() {
        Season s1 = mock(Season.class);
        Season s2 = mock(Season.class);
        SeasonResponse r1 = mock(SeasonResponse.class);
        SeasonResponse r2 = mock(SeasonResponse.class);

        when(listUC.execute()).thenReturn(List.of(s1, s2));
        when(mapper.toResponse(s1)).thenReturn(r1);
        when(mapper.toResponse(s2)).thenReturn(r2);

        ResponseEntity<List<SeasonResponse>> result = controller.list();

        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        assertSame(r1, result.getBody().get(0));
        assertSame(r2, result.getBody().get(1));
    }

    @Test
    void deleteShouldCallUseCaseAndReturnNoContent() {
        Long id = 77L;

        ResponseEntity<Void> result = controller.delete(id);

        verify(deleteUC, times(1)).execute(eq(id));
        assertEquals(204, result.getStatusCodeValue());
        assertNull(result.getBody());
    }
}