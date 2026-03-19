package com.cine.cinelog.features.watchentry.web.controller;

import com.cine.cinelog.core.application.usecase.watchentry.CreateWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.DeleteWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.GetWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.ListWatchEntriesService;
import com.cine.cinelog.core.application.usecase.watchentry.UpdateWatchEntryService;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.features.watchentry.mapper.WatchEntryMapper;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryCreateRequest;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryResponse;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.shared.web.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchEntryControllerTest {

    @Mock
    private CreateWatchEntryService createUC;
    @Mock
    private ListWatchEntriesService listUC;
    @Mock
    private UpdateWatchEntryService updateUC;
    @Mock
    private GetWatchEntryService getUC;
    @Mock
    private DeleteWatchEntryService deleteUC;
    @Mock
    private WatchEntryMapper watchEntryMapper;

    @Mock
    private com.cine.cinelog.shared.observability.metrics.BusinessMetricsService metricsService;

    @InjectMocks
    private WatchEntryController controller;

    @Captor
    private ArgumentCaptor<PageRequest> pageRequestCaptor;

    @BeforeEach
    void setUp() {
        // controller is injected by @InjectMocks
    }

    @Test
    void create_shouldReturnCreatedResponseWithLocationAndBody() {
        WatchEntryCreateRequest req = mock(WatchEntryCreateRequest.class);
        WatchEntry domain = mock(WatchEntry.class);
        WatchEntry saved = mock(WatchEntry.class);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);

        when(watchEntryMapper.toDomain(req)).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(saved);
        when(saved.getId()).thenReturn(42L);
        when(watchEntryMapper.toResponse(saved)).thenReturn(resp);

        ResponseEntity<WatchEntryResponse> response = controller.create(req);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("/api/watch/42", response.getHeaders().getLocation().toString());
        assertSame(resp, response.getBody());

        verify(watchEntryMapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(watchEntryMapper).toResponse(saved);
    }

    @Test
    void list_shouldCallListUseCaseAndReturnPageResponse() {
        Long userId = 7L;
        Long mediaId = 11L;
        Long episodeId = null;
        Integer minRating = 3;
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);
        int page = 0;
        int size = 20;

        WatchEntry entry = mock(WatchEntry.class);
        WatchEntryResponse dto = mock(WatchEntryResponse.class);

        var pageResult = new PageResult<>(List.of(entry), page, size, 1L, 1);
        when(listUC.execute(eq(userId), eq(mediaId), eq(episodeId), eq(minRating), eq(from), eq(to),
                any(PageRequest.class)))
                .thenReturn(pageResult);
        when(watchEntryMapper.toResponse(entry)).thenReturn(dto);

        ResponseEntity<PageResponse<WatchEntryResponse>> response = controller.list(userId, mediaId, episodeId,
                minRating, from, to, page, size);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        // ensure use case called
        verify(listUC).execute(eq(userId), eq(mediaId), eq(episodeId), eq(minRating), eq(from), eq(to),
                pageRequestCaptor.capture());
        PageRequest captured = pageRequestCaptor.getValue();
        assertEquals(page, captured.getPageNumber());
        assertEquals(size, captured.getPageSize());
        verify(watchEntryMapper).toResponse(entry);
    }

    @Test
    void update_shouldCallUpdateAndReturnUpdatedResponse() {
        Long id = 5L;
        WatchEntryCreateRequest req = mock(WatchEntryCreateRequest.class);
        WatchEntry domain = mock(WatchEntry.class);
        WatchEntry updated = mock(WatchEntry.class);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);

        when(watchEntryMapper.toDomain(req)).thenReturn(domain);
        when(updateUC.execute(id, domain, true)).thenReturn(updated);
        when(watchEntryMapper.toResponse(updated)).thenReturn(resp);

        ResponseEntity<WatchEntryResponse> response = controller.update(id, req);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(resp, response.getBody());

        verify(watchEntryMapper).toDomain(req);
        verify(updateUC).execute(id, domain, true);
        verify(watchEntryMapper).toResponse(updated);
    }

    @Test
    void delete_shouldCallDeleteUseCaseAndReturnNoContent() {
        Long id = 99L;

        ResponseEntity<Void> response = controller.delete(id);

        assertEquals(204, response.getStatusCodeValue());
        verify(deleteUC).execute(id);
    }

    @Test
    void getById_shouldReturnEntryResponse() {
        Long id = 123L;
        WatchEntry entry = mock(WatchEntry.class);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);

        when(getUC.execute(id)).thenReturn(entry);
        when(watchEntryMapper.toResponse(entry)).thenReturn(resp);

        ResponseEntity<WatchEntryResponse> response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(resp, response.getBody());
        verify(getUC).execute(id);
        verify(watchEntryMapper).toResponse(entry);
    }
}
