package com.cine.cinelog.features.credits.web.controller;

import com.cine.cinelog.core.application.ports.in.credits.CreateCreditUseCase;
import com.cine.cinelog.core.application.ports.in.credits.DeleteCreditUseCase;
import com.cine.cinelog.core.application.ports.in.credits.GetCreditUseCase;
import com.cine.cinelog.core.application.ports.in.credits.ListCreditsUseCase;
import com.cine.cinelog.core.application.ports.in.credits.UpdateCreditUseCase;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.web.dto.CreditCreateRequest;
import com.cine.cinelog.features.credits.web.dto.CreditResponse;
import com.cine.cinelog.features.credits.web.dto.CreditUpdateRequest;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditControllerTest {

    private CreateCreditUseCase createUC;
    private UpdateCreditUseCase updateUC;
    private GetCreditUseCase getUC;
    private ListCreditsUseCase listUC;
    private DeleteCreditUseCase deleteUC;
    private CreditMapper mapper;

    private CreditController controller;
    private BusinessMetricsService metricsService;

    @BeforeEach
    void setUp() {
        createUC = mock(CreateCreditUseCase.class);
        updateUC = mock(UpdateCreditUseCase.class);
        getUC = mock(GetCreditUseCase.class);
        listUC = mock(ListCreditsUseCase.class);
        deleteUC = mock(DeleteCreditUseCase.class);
        mapper = mock(CreditMapper.class);
        metricsService = mock(BusinessMetricsService.class);

        controller = new CreditController(createUC, updateUC, getUC, listUC, deleteUC, mapper, metricsService);
    }

    @Test
    void create_shouldReturnCreatedResponse() {
        CreditCreateRequest req = mock(CreditCreateRequest.class);
        Credit domain = mock(Credit.class);
        CreditResponse resp = mock(CreditResponse.class);

        when(req.role()).thenReturn("ACTOR");
        when(mapper.toDomain(req)).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(domain);
        when(domain.getId()).thenReturn(1L);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<CreditResponse> result = controller.create(req);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());
        assertNotNull(result.getHeaders().getLocation());
        assertTrue(result.getHeaders().getLocation().toString().endsWith("/api/credits/1"));

        verify(mapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(mapper).toResponse(domain);
    }

    @Test
    void update_shouldReturnOkResponse() {
        Long id = 1L;
        CreditUpdateRequest req = mock(CreditUpdateRequest.class);
        Credit domain = mock(Credit.class);
        CreditResponse resp = mock(CreditResponse.class);

        when(req.role()).thenReturn("DIRECTOR");
        when(mapper.toDomain(req)).thenReturn(domain);
        when(updateUC.execute(id, domain)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<CreditResponse> result = controller.update(id, req);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domain);
        verify(mapper).toResponse(domain);
    }

    @Test
    void getById_shouldReturnOkResponse() {
        Long id = 1L;
        Credit domain = mock(Credit.class);
        CreditResponse resp = mock(CreditResponse.class);

        when(getUC.execute(id)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<CreditResponse> result = controller.getById(id);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(domain);
    }

    @Test
    void delete_shouldReturnNoContent() {
        Long id = 1L;
        doNothing().when(deleteUC).execute(id);

        ResponseEntity<Void> result = controller.delete(id);

        assertEquals(204, result.getStatusCodeValue());
        assertNull(result.getBody());

        verify(deleteUC).execute(id);
    }
}
