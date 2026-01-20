package com.cine.cinelog.features.seasons.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
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
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonControllerTest {

    @Mock
    private CreateSeasonUseCase createUC;

    @Mock
    private UpdateSeasonUseCase updateUC;

    @Mock
    private GetSeasonUseCase getUC;

    @Mock
    private ListSeasonsUseCase listUC;

    @Mock
    private DeleteSeasonUseCase deleteUC;

    @Mock
    private SeasonMapper mapper;

    @Mock
    private com.cine.cinelog.shared.observability.metrics.BusinessMetricsService metricsService;

    @InjectMocks
    private SeasonController controller;

    @Test
    void create_shouldReturnCreatedResponse() {
        SeasonCreateRequest req = mock(SeasonCreateRequest.class);
        Season domain = mock(Season.class);
        Season created = mock(Season.class);
        SeasonResponse response = mock(SeasonResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(created);
        when(created.getId()).thenReturn(42L);
        when(mapper.toResponse(created)).thenReturn(response);

        ResponseEntity<SeasonResponse> resp = controller.create(req);

        assertEquals(201, resp.getStatusCodeValue());
        assertEquals(URI.create("/api/seasons/42"), resp.getHeaders().getLocation());
        assertSame(response, resp.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(mapper).toResponse(created);
    }

    @Test
    void update_shouldReturnOkResponse() {
        Long id = 7L;
        SeasonUpdateRequest req = mock(SeasonUpdateRequest.class);
        Season domain = mock(Season.class);
        Season updated = mock(Season.class);
        SeasonResponse response = mock(SeasonResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(updateUC.execute(id, domain)).thenReturn(updated);
        when(mapper.toResponse(updated)).thenReturn(response);

        ResponseEntity<SeasonResponse> resp = controller.update(id, req);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(response, resp.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domain);
        verify(mapper).toResponse(updated);
    }

    @Test
    void getById_shouldReturnOkResponse() {
        Long id = 5L;
        Season season = mock(Season.class);
        SeasonResponse response = mock(SeasonResponse.class);

        when(getUC.execute(id)).thenReturn(season);
        when(mapper.toResponse(season)).thenReturn(response);

        ResponseEntity<SeasonResponse> resp = controller.getById(id);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(response, resp.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(season);
    }

    @Test
    void delete_shouldReturnNoContent() {
        Long id = 3L;

        ResponseEntity<Void> resp = controller.delete(id);

        assertEquals(204, resp.getStatusCodeValue());
        assertNull(resp.getBody());

        verify(deleteUC).execute(id);
    }

    @Test
    void list_shouldReturnPageResponse() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        @SuppressWarnings("unchecked")
        PageResult<Season> pageResult = mock(PageResult.class);
        @SuppressWarnings("rawtypes")
        PageResponse pageResponse = mock(PageResponse.class);

        when(listUC.execute(any(PageQuery.class))).thenReturn(pageResult);

        try (MockedStatic<PageResponseMapper> mocked = mockStatic(PageResponseMapper.class)) {
            // stub the static mapper to return our mocked page response
            mocked.when(() -> PageResponseMapper.from(eq(pageResult), any()))
                    .thenReturn(pageResponse);

            ResponseEntity<PageResponse<SeasonResponse>> resp = controller.list(pageable);

            assertEquals(200, resp.getStatusCodeValue());
            assertSame(pageResponse, resp.getBody());

            verify(listUC).execute(any(PageQuery.class));
            // ensure mapper instance was provided to the static call (can't directly verify
            // lambda)
            mocked.verify(() -> PageResponseMapper.from(eq(pageResult), any()));
        }
    }
}