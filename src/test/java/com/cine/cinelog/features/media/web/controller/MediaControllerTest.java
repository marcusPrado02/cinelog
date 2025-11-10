package com.cine.cinelog.features.media.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.features.media.web.dto.MediaUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private CreateMediaUseCase createUseCase;
    @Mock
    private UpdateMediaUseCase updateUseCase;
    @Mock
    private GetMediaUseCase getUseCase;
    @Mock
    private ListMediaUseCase listUseCase;
    @Mock
    private DeleteMediaUseCase deleteUseCase;
    @Mock
    private MediaMapper mapper;

    @InjectMocks
    private MediaController controller;

    @Captor
    private ArgumentCaptor<Long> longCaptor;

    @BeforeEach
    void setUp() {
        // controller is injected by Mockito @InjectMocks
    }

    @Test
    void create_should_return_created_with_location_and_body() {
        MediaCreateRequest req = mock(MediaCreateRequest.class);
        Media domain = mock(Media.class);
        when(domain.getId()).thenReturn(123L);
        MediaResponse resp = mock(MediaResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(createUseCase.execute(domain)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<MediaResponse> result = controller.create(req);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(resp, result.getBody());
        assertTrue(result.getHeaders().getLocation().toString().endsWith("/api/media/123"));
    }

    @Test
    void get_should_return_ok_with_mapped_response() {
        long id = 1L;
        Media domain = mock(Media.class);
        MediaResponse resp = mock(MediaResponse.class);

        when(getUseCase.execute(id)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<MediaResponse> result = controller.get(id);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(resp, result.getBody());
    }

    @Test
    void list_should_return_ok_with_mapped_list() {
        Media domain = mock(Media.class);
        MediaResponse resp = mock(MediaResponse.class);

        when(listUseCase.execute(MediaType.MOVIE, "query", 0, 10)).thenReturn(List.of(domain));
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<List<MediaResponse>> result = controller.list(MediaType.MOVIE, "query", 0, 10);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals(resp, result.getBody().get(0));
    }

    @Test
    void update_should_return_ok_with_mapped_response() {
        long id = 5L;
        MediaUpdateRequest req = mock(MediaUpdateRequest.class);
        Media domain = mock(Media.class);
        MediaResponse resp = mock(MediaResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(updateUseCase.execute(id, domain)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<MediaResponse> result = controller.update(id, req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(resp, result.getBody());
    }

    @Test
    void delete_should_invoke_usecase() {
        long id = 9L;

        // no stubbing needed for void method
        controller.delete(id);

        verify(deleteUseCase).execute(longCaptor.capture());
        assertEquals(id, longCaptor.getValue());
    }

    @Test
    void notFound_handler_should_return_problem_detail_with_message_and_404() {
        IllegalArgumentException ex = new IllegalArgumentException("not found");
        ProblemDetail pd = controller.notFound(ex);

        assertNotNull(pd);
        assertEquals(HttpStatus.NOT_FOUND.value(), pd.getStatus());
        assertEquals("not found", pd.getDetail());
    }
}
