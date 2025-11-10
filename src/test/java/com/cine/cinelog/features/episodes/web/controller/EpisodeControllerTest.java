package com.cine.cinelog.features.episodes.web.controller;

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
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EpisodeControllerTest {

    private final CreateEpisodeUseCase createUC = mock(CreateEpisodeUseCase.class);
    private final UpdateEpisodeUseCase updateUC = mock(UpdateEpisodeUseCase.class);
    private final GetEpisodeUseCase getUC = mock(GetEpisodeUseCase.class);
    private final ListEpisodesUseCase listUC = mock(ListEpisodesUseCase.class);
    private final DeleteEpisodeUseCase deleteUC = mock(DeleteEpisodeUseCase.class);
    private final EpisodeMapper mapper = mock(EpisodeMapper.class);

    private final EpisodeController controller = new EpisodeController(
            createUC, updateUC, getUC, listUC, deleteUC, mapper);

    @Test
    void create_shouldReturnCreatedResponse_withLocationAndBody() {
        EpisodeCreateRequest req = mock(EpisodeCreateRequest.class);
        Episode domain = mock(Episode.class);
        EpisodeResponse dto = mock(EpisodeResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(domain);
        when(domain.getId()).thenReturn(123L);
        when(mapper.toResponse(domain)).thenReturn(dto);

        var response = controller.create(req);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(URI.create("/api/episodes/123"), response.getHeaders().getLocation());
        assertSame(dto, response.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(mapper).toResponse(domain);
    }

    @Test
    void update_shouldReturnOk_withMappedBody() {
        Long id = 1L;
        EpisodeUpdateRequest req = mock(EpisodeUpdateRequest.class);
        Episode domain = mock(Episode.class);
        EpisodeResponse dto = mock(EpisodeResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(updateUC.execute(id, domain)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(dto);

        var response = controller.update(id, req);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(dto, response.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domain);
        verify(mapper).toResponse(domain);
    }

    @Test
    void getById_shouldReturnOk_withMappedBody() {
        Long id = 2L;
        Episode domain = mock(Episode.class);
        EpisodeResponse dto = mock(EpisodeResponse.class);

        when(getUC.execute(id)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(dto);

        var response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(dto, response.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(domain);
    }

    @Test
    void list_shouldReturnOk_withMappedList() {
        Episode e1 = mock(Episode.class);
        Episode e2 = mock(Episode.class);
        EpisodeResponse r1 = mock(EpisodeResponse.class);
        EpisodeResponse r2 = mock(EpisodeResponse.class);

        when(listUC.execute()).thenReturn(List.of(e1, e2));
        when(mapper.toResponse(e1)).thenReturn(r1);
        when(mapper.toResponse(e2)).thenReturn(r2);

        var response = controller.list();

        assertEquals(200, response.getStatusCodeValue());
        List<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertSame(r1, body.get(0));
        assertSame(r2, body.get(1));

        verify(listUC).execute();
        verify(mapper).toResponse(e1);
        verify(mapper).toResponse(e2);
    }

    @Test
    void delete_shouldCallUseCase_andReturnNoContent() {
        Long id = 5L;

        var response = controller.delete(id);

        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());

        verify(deleteUC).execute(id);
    }
}