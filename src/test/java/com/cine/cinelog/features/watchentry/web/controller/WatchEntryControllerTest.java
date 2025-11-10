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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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
    private WatchEntryMapper mapper;

    private WatchEntryController controller;

    @BeforeEach
    void setUp() {
        controller = new WatchEntryController(createUC, listUC, updateUC, getUC, deleteUC, mapper);
    }

    @Test
    void create_returnsCreatedResponse_withLocationAndBody() {
        WatchEntryCreateRequest req = mock(WatchEntryCreateRequest.class);
        WatchEntry domain = mock(WatchEntry.class);
        when(domain.getId()).thenReturn(42L);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);

        when(mapper.toDomain(any(WatchEntryCreateRequest.class))).thenReturn(domain);
        when(createUC.execute(any())).thenReturn(domain);
        when(mapper.toResponse(any())).thenReturn(resp);

        ResponseEntity<WatchEntryResponse> response = controller.create(req);

        assertEquals(201, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/api/watch/42"));
        assertSame(resp, response.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(mapper).toResponse(domain);
    }

    @Test
    void list_returnsPageOfMappedResponses() {
        WatchEntry domain = mock(WatchEntry.class);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);
        Page<WatchEntry> page = new PageImpl<>(List.of(domain));

        when(listUC.execute(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);
        when(mapper.toResponse(any())).thenReturn(resp);

        ResponseEntity<Page<WatchEntryResponse>> response = controller.list(
                10L, null, null, null, null, null, 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        Page<WatchEntryResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getTotalElements());
        assertSame(resp, body.getContent().get(0));

        verify(listUC).execute(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class));
        verify(mapper).toResponse(domain);
    }

    @Test
    void update_callsUpdateAndReturnsMappedResponse() {
        Long id = 7L;
        WatchEntryCreateRequest req = mock(WatchEntryCreateRequest.class);
        WatchEntry domain = mock(WatchEntry.class);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);

        when(mapper.toDomain((WatchEntryCreateRequest) any())).thenReturn(domain);
        when(updateUC.execute(any(), eq(true))).thenReturn(domain);
        when(mapper.toResponse(any())).thenReturn(resp);

        ResponseEntity<WatchEntryResponse> response = controller.update(id, req);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(resp, response.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(domain, true);
        verify(mapper).toResponse(domain);
    }

    @Test
    void delete_callsDeleteAndReturnsNoContent() {
        Long id = 99L;
        doNothing().when(deleteUC).execute(id);

        ResponseEntity<Void> response = controller.delete(id);

        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());

        verify(deleteUC).execute(id);
    }

    @Test
    void getById_returnsMappedResponse() {
        Long id = 5L;
        WatchEntry domain = mock(WatchEntry.class);
        WatchEntryResponse resp = mock(WatchEntryResponse.class);

        when(getUC.execute(id)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<WatchEntryResponse> response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertSame(resp, response.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(domain);
    }
}