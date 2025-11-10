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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditControllerTest {

    @Mock
    private CreateCreditUseCase createUC;

    @Mock
    private UpdateCreditUseCase updateUC;

    @Mock
    private GetCreditUseCase getUC;

    @Mock
    private ListCreditsUseCase listUC;

    @Mock
    private DeleteCreditUseCase deleteUC;

    @Mock
    private CreditMapper mapper;

    @InjectMocks
    private CreditController controller;

    @Captor
    private ArgumentCaptor<Long> longCaptor;

    @Test
    void create_shouldReturnCreatedResponseWithLocationAndBody() {
        CreditCreateRequest req = mock(CreditCreateRequest.class);
        Credit domainFromReq = mock(Credit.class);
        Credit createdDomain = mock(Credit.class);
        CreditResponse expectedResponse = mock(CreditResponse.class);

        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(createUC.execute(domainFromReq)).thenReturn(createdDomain);
        when(createdDomain.getId()).thenReturn(123L);
        when(mapper.toResponse(createdDomain)).thenReturn(expectedResponse);

        ResponseEntity<CreditResponse> resp = controller.create(req);

        assertEquals(201, resp.getStatusCodeValue());
        assertEquals(URI.create("/api/credits/123"), resp.getHeaders().getLocation());
        assertSame(expectedResponse, resp.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(domainFromReq);
        verify(mapper).toResponse(createdDomain);
    }

    @Test
    void update_shouldReturnOkWithUpdatedBody() {
        Long id = 5L;
        CreditUpdateRequest req = mock(CreditUpdateRequest.class);
        Credit domainFromReq = mock(Credit.class);
        Credit updatedDomain = mock(Credit.class);
        CreditResponse expectedResponse = mock(CreditResponse.class);

        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(updateUC.execute(id, domainFromReq)).thenReturn(updatedDomain);
        when(mapper.toResponse(updatedDomain)).thenReturn(expectedResponse);

        ResponseEntity<CreditResponse> resp = controller.update(id, req);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(expectedResponse, resp.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domainFromReq);
        verify(mapper).toResponse(updatedDomain);
    }

    @Test
    void getById_shouldReturnOkWithMappedBody() {
        Long id = 7L;
        Credit domain = mock(Credit.class);
        CreditResponse expectedResponse = mock(CreditResponse.class);

        when(getUC.execute(id)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(expectedResponse);

        ResponseEntity<CreditResponse> resp = controller.getById(id);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(expectedResponse, resp.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(domain);
    }

    @Test
    void list_shouldReturnOkWithMappedList() {
        Credit domain = mock(Credit.class);
        CreditResponse expectedResponse = mock(CreditResponse.class);

        when(listUC.execute()).thenReturn(List.of(domain));
        when(mapper.toResponse(domain)).thenReturn(expectedResponse);

        ResponseEntity<java.util.List<CreditResponse>> resp = controller.list();

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertSame(expectedResponse, resp.getBody().get(0));

        verify(listUC).execute();
        verify(mapper).toResponse(domain);
    }

    @Test
    void delete_shouldInvokeUseCaseAndReturnNoContent() {
        Long id = 9L;

        ResponseEntity<Void> resp = controller.delete(id);

        assertEquals(204, resp.getStatusCodeValue());
        assertNull(resp.getBody());

        verify(deleteUC).execute(longCaptor.capture());
        assertEquals(id, longCaptor.getValue());
    }
}