package com.cine.cinelog.features.media.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.URI;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.RecommendMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.SearchMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.features.media.web.dto.MediaSearchRequest;
import com.cine.cinelog.features.media.web.dto.MediaUpdateRequest;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;
import com.cine.cinelog.shared.web.dto.PageResponse;
import com.cine.cinelog.shared.web.dto.PageResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import com.cine.cinelog.core.application.pagination.PageQuery;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private CreateMediaUseCase createUC;
    @Mock
    private UpdateMediaUseCase updateUC;
    @Mock
    private GetMediaUseCase getUC;
    @Mock
    private SearchMediaUseCase searchUC;
    @Mock
    private ListMediaUseCase listUC;
    @Mock
    private DeleteMediaUseCase deleteUC;
    @Mock
    private RecommendMediaUseCase recommendUC;
    @Mock
    private MediaMapper mapper;

    private MediaController controller;

    @Mock
    private BusinessMetricsService metricsService;

    @BeforeEach
    void setUp() {
        controller = new MediaController(createUC, updateUC, getUC, listUC, deleteUC, searchUC, recommendUC, mapper,
                metricsService);
    }

    @Test
    void createUC_shouldReturnCreatedWithLocationAndBody() {
        MediaCreateRequest req = mock(MediaCreateRequest.class);
        when(req.getTitle()).thenReturn("Test Movie");
        when(req.getType()).thenReturn(com.cine.cinelog.core.domain.enums.MediaType.MOVIE);

        Media saved = mock(Media.class);
        when(saved.getId()).thenReturn(42L);
        when(saved.getTitle()).thenReturn("Test Movie");
        when(saved.getType()).thenReturn(com.cine.cinelog.core.domain.enums.MediaType.MOVIE);
        MediaResponse respDto = mock(MediaResponse.class);

        when(mapper.toDomain(req)).thenReturn(saved);
        when(createUC.execute(saved)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(respDto);

        ResponseEntity<MediaResponse> response = controller.createUC(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(URI.create("/api/v1/media/42"), response.getHeaders().getLocation());
        assertSame(respDto, response.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(saved);
        verify(mapper).toResponse(saved);
    }

    @Test
    void getUC_shouldReturnOkWithBody() {
        long id = 7L;
        Media domain = mock(Media.class);
        MediaResponse dto = mock(MediaResponse.class);

        when(getUC.execute(id)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(dto);

        ResponseEntity<MediaResponse> response = controller.getUC(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(getUC).execute(id);
        verify(mapper).toResponse(domain);
    }

    @Test
    void updateUC_shouldReturnOkWithUpdatedBody() {
        long id = 9L;
        MediaUpdateRequest req = mock(MediaUpdateRequest.class);
        when(req.title()).thenReturn("Updated Title");
        when(req.type()).thenReturn(com.cine.cinelog.core.domain.enums.MediaType.MOVIE);

        Media domainFromReq = mock(Media.class);
        Media updatedDomain = mock(Media.class);
        MediaResponse dto = mock(MediaResponse.class);

        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(updateUC.execute(id, domainFromReq)).thenReturn(updatedDomain);
        when(mapper.toResponse(updatedDomain)).thenReturn(dto);

        ResponseEntity<MediaResponse> response = controller.updateUC(id, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domainFromReq);
        verify(mapper).toResponse(updatedDomain);
    }

    @Test
    void deleteUC_shouldCallUseCase() {
        long id = 11L;
        doNothing().when(deleteUC).execute(id);

        controller.deleteUC(id);

        verify(deleteUC).execute(id);
    }

    @Test
    void listUC_shouldReturnPageResponseFromMapper() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageResult<Media> mockedResult = mock(PageResult.class);
        @SuppressWarnings("unchecked")
        PageResponse<MediaResponse> mockedPageResponse = mock(PageResponse.class);

        when(listUC.execute(any(PageQuery.class))).thenReturn(mockedResult);

        try (MockedStatic<PageResponseMapper> utilities = mockStatic(PageResponseMapper.class)) {
            utilities.when(() -> PageResponseMapper.from(any(), any())).thenReturn(mockedPageResponse);

            ResponseEntity<PageResponse<MediaResponse>> response = controller.listUC(pageable);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(mockedPageResponse, response.getBody());
            utilities.verify(() -> PageResponseMapper.from(eq(mockedResult), any()));
            verify(listUC).execute(any(PageQuery.class));
        }
    }

    @Test
    void searchUC_shouldBuildCriteriaAndCallUseCase() {
        MediaSearchRequest req = mock(MediaSearchRequest.class);
        when(req.getText()).thenReturn("abc");
        when(req.getType()).thenReturn(com.cine.cinelog.core.domain.enums.MediaType.MOVIE);
        when(req.getYearMin()).thenReturn(2000);
        when(req.getYearMax()).thenReturn(2020);
        when(req.getRatingMin()).thenReturn(1.5);
        when(req.getRatingMax()).thenReturn(9.5);
        when(req.getGenreIds()).thenReturn(java.util.List.of());
        when(req.getPage()).thenReturn(2);
        when(req.getSize()).thenReturn(10);

        PageResult<Media> mockedResult = mock(PageResult.class);
        @SuppressWarnings("unchecked")
        PageResponse<MediaResponse> mockedPageResponse = mock(PageResponse.class);

        when(searchUC.execute(any(MediaSearchCriteria.class), any(PageQuery.class))).thenReturn(mockedResult);

        try (MockedStatic<PageResponseMapper> utilities = mockStatic(PageResponseMapper.class)) {
            utilities.when(() -> PageResponseMapper.from(any(), any())).thenReturn(mockedPageResponse);

            ResponseEntity<PageResponse<MediaResponse>> response = controller.searchUC(req);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(mockedPageResponse, response.getBody());

            ArgumentCaptor<MediaSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(MediaSearchCriteria.class);
            ArgumentCaptor<PageQuery> pageQueryCaptor = ArgumentCaptor.forClass(PageQuery.class);
            verify(searchUC).execute(criteriaCaptor.capture(), pageQueryCaptor.capture());

            MediaSearchCriteria criteria = criteriaCaptor.getValue();
            assertEquals("abc", criteria.getText());
            assertEquals(Integer.valueOf(2000), criteria.getYearMin());
            assertEquals(Integer.valueOf(2020), criteria.getYearMax());
            assertEquals(Double.valueOf(1.5), criteria.getRatingMin());
            assertEquals(Double.valueOf(9.5), criteria.getRatingMax());

            PageQuery pq = pageQueryCaptor.getValue();
            assertEquals(2, pq.page());
            assertEquals(10, pq.size());
        }
    }

    @Test
    void notFound_shouldReturnProblemDetailWithMessageAnd404() {
        IllegalArgumentException ex = new IllegalArgumentException("not found");
        ProblemDetail pd = controller.notFound(ex);

        assertNotNull(pd);
        assertEquals(404, pd.getStatus());
        assertEquals("not found", pd.getDetail());
    }
}
