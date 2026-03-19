package com.cine.cinelog.features.episodes.web.controller;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.episodes.CreateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.DeleteEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.GetEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.ListEpisodesUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.UpdateEpisodeUseCase;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.web.dto.EpisodeCreateRequest;
import com.cine.cinelog.features.episodes.web.dto.EpisodeResponse;
import com.cine.cinelog.features.episodes.web.dto.EpisodeUpdateRequest;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import com.cine.cinelog.shared.web.dto.PageableMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EpisodeControllerTest {

    @Test
    public void create_shouldReturnCreatedResponse() {
        CreateEpisodeUseCase createUC = mock(CreateEpisodeUseCase.class);
        UpdateEpisodeUseCase updateUC = mock(UpdateEpisodeUseCase.class);
        GetEpisodeUseCase getUC = mock(GetEpisodeUseCase.class);
        ListEpisodesUseCase listUC = mock(ListEpisodesUseCase.class);
        DeleteEpisodeUseCase deleteUC = mock(DeleteEpisodeUseCase.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        BusinessMetricsService metricsService = mock(BusinessMetricsService.class);

        EpisodeController controller = new EpisodeController(createUC, updateUC, getUC, listUC, deleteUC, mapper,
                metricsService);

        EpisodeCreateRequest req = mock(EpisodeCreateRequest.class);
        Episode domain = mock(Episode.class);
        when(req.name()).thenReturn("Pilot");
        when(req.seasonId()).thenReturn(1L);
        when(mapper.toDomain(req)).thenReturn(domain);

        Episode created = mock(Episode.class);
        when(created.getId()).thenReturn(42L);
        when(createUC.execute(domain)).thenReturn(created);

        EpisodeResponse resp = mock(EpisodeResponse.class);
        when(mapper.toResponse(created)).thenReturn(resp);

        ResponseEntity<EpisodeResponse> response = controller.create(req);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(URI.create("/api/episodes/42"), response.getHeaders().getLocation());
        assertSame(resp, response.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(mapper).toResponse(created);
    }

    @Test
    public void update_shouldReturnOkWithUpdated() {
        CreateEpisodeUseCase createUC = mock(CreateEpisodeUseCase.class);
        UpdateEpisodeUseCase updateUC = mock(UpdateEpisodeUseCase.class);
        GetEpisodeUseCase getUC = mock(GetEpisodeUseCase.class);
        ListEpisodesUseCase listUC = mock(ListEpisodesUseCase.class);
        DeleteEpisodeUseCase deleteUC = mock(DeleteEpisodeUseCase.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        BusinessMetricsService metricsService = mock(BusinessMetricsService.class);

        EpisodeController controller = new EpisodeController(createUC, updateUC, getUC, listUC, deleteUC, mapper,
                metricsService);

        Long id = 7L;
        EpisodeUpdateRequest req = mock(EpisodeUpdateRequest.class);
        Episode domain = mock(Episode.class);
        when(req.name()).thenReturn("Updated Episode");
        when(mapper.toDomain(req)).thenReturn(domain);

        Episode updated = mock(Episode.class);
        when(updateUC.execute(eq(id), eq(domain))).thenReturn(updated);

        EpisodeResponse resp = mock(EpisodeResponse.class);
        when(mapper.toResponse(updated)).thenReturn(resp);

        ResponseEntity<EpisodeResponse> response = controller.update(id, req);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(resp, response.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domain);
        verify(mapper).toResponse(updated);
    }

    @Test
    public void getById_shouldReturnOkWithEpisode() {
        CreateEpisodeUseCase createUC = mock(CreateEpisodeUseCase.class);
        UpdateEpisodeUseCase updateUC = mock(UpdateEpisodeUseCase.class);
        GetEpisodeUseCase getUC = mock(GetEpisodeUseCase.class);
        ListEpisodesUseCase listUC = mock(ListEpisodesUseCase.class);
        DeleteEpisodeUseCase deleteUC = mock(DeleteEpisodeUseCase.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        BusinessMetricsService metricsService = mock(BusinessMetricsService.class);

        EpisodeController controller = new EpisodeController(createUC, updateUC, getUC, listUC, deleteUC, mapper,
                metricsService);

        Long id = 13L;
        Episode domain = mock(Episode.class);
        when(getUC.execute(id)).thenReturn(domain);

        EpisodeResponse resp = mock(EpisodeResponse.class);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<EpisodeResponse> response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(resp, response.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(domain);
    }

    @Test
    public void list_shouldReturnPageResponse() {
        CreateEpisodeUseCase createUC = mock(CreateEpisodeUseCase.class);
        UpdateEpisodeUseCase updateUC = mock(UpdateEpisodeUseCase.class);
        GetEpisodeUseCase getUC = mock(GetEpisodeUseCase.class);
        ListEpisodesUseCase listUC = mock(ListEpisodesUseCase.class);
        DeleteEpisodeUseCase deleteUC = mock(DeleteEpisodeUseCase.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        BusinessMetricsService metricsService = mock(BusinessMetricsService.class);

        EpisodeController controller = new EpisodeController(createUC, updateUC, getUC, listUC, deleteUC, mapper,
                metricsService);

        Pageable pageable = mock(Pageable.class);
        PageQuery pageQuery = mock(PageQuery.class);
        PageResult<Episode> pageResult = mock(PageResult.class);
        @SuppressWarnings("unchecked")
        PageResponse<EpisodeResponse> pageResponse = mock(PageResponse.class);

        // Mock static PageableMapper.toPageQuery and PageResponseMapper.from
        try (MockedStatic<PageableMapper> pm = Mockito.mockStatic(PageableMapper.class);
                MockedStatic<PageResponseMapper> prm = Mockito.mockStatic(PageResponseMapper.class)) {
            pm.when(() -> PageableMapper.toPageQuery(pageable)).thenReturn(pageQuery);
            when(listUC.execute(pageQuery)).thenReturn(pageResult);
            // PageResponseMapper.from(result, mapper::toResponse)
            prm.when(() -> PageResponseMapper.from(eq(pageResult), any())).thenReturn(pageResponse);

            ResponseEntity<PageResponse<EpisodeResponse>> response = controller.list(pageable);

            assertEquals(200, response.getStatusCodeValue());
            assertSame(pageResponse, response.getBody());

            pm.verify(() -> PageableMapper.toPageQuery(pageable));
            verify(listUC).execute(pageQuery);
            prm.verify(() -> PageResponseMapper.from(eq(pageResult), any()));
        }
    }

    @Test
    public void delete_shouldInvokeUseCaseAndReturnNoContent() {
        CreateEpisodeUseCase createUC = mock(CreateEpisodeUseCase.class);
        UpdateEpisodeUseCase updateUC = mock(UpdateEpisodeUseCase.class);
        GetEpisodeUseCase getUC = mock(GetEpisodeUseCase.class);
        ListEpisodesUseCase listUC = mock(ListEpisodesUseCase.class);
        DeleteEpisodeUseCase deleteUC = mock(DeleteEpisodeUseCase.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        BusinessMetricsService metricsService = mock(BusinessMetricsService.class);

        EpisodeController controller = new EpisodeController(createUC, updateUC, getUC, listUC, deleteUC, mapper,
                metricsService);

        Long id = 99L;

        ResponseEntity<Void> response = controller.delete(id);

        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());

        verify(deleteUC).execute(id);
    }
}
