package com.cine.cinelog.features.people.web.controller;

import com.cine.cinelog.core.application.ports.in.person.CreatePersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.DeletePersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.GetPersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.ListPeopleUseCase;
import com.cine.cinelog.core.application.ports.in.person.SearchPeopleUseCase;
import com.cine.cinelog.core.application.ports.in.person.UpdatePersonUseCase;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.web.dto.PersonCreateRequest;
import com.cine.cinelog.features.people.web.dto.PersonResponse;
import com.cine.cinelog.features.people.web.dto.PersonUpdateRequest;
import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private CreatePersonUseCase createUC;
    @Mock
    private UpdatePersonUseCase updateUC;
    @Mock
    private GetPersonUseCase getUC;
    @Mock
    private ListPeopleUseCase listUC;
    @Mock
    private DeletePersonUseCase deleteUC;
    @Mock
    private SearchPeopleUseCase searchUC;
    @Mock
    private PersonMapper mapper;
    @Mock
    private BusinessMetricsService metricsService;

    private PersonController controller;

    @BeforeEach
    void setUp() {
        controller = new PersonController(createUC, updateUC, getUC, listUC, deleteUC, searchUC, mapper,
                metricsService);
    }

    @Test
    void create_shouldReturnCreatedResponse_withLocationAndBody() {
        PersonCreateRequest req = mock(PersonCreateRequest.class);
        Person domain = mock(Person.class);
        Person created = mock(Person.class);
        PersonResponse response = mock(PersonResponse.class);

        when(req.name()).thenReturn("Test Person");
        when(mapper.toDomain(req)).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(created);
        when(created.getId()).thenReturn(42L);
        when(mapper.toResponse(created)).thenReturn(response);

        ResponseEntity<PersonResponse> resp = controller.create(req);

        assertEquals(201, resp.getStatusCodeValue());
        assertEquals("/api/people/42", resp.getHeaders().getLocation().getPath());
        assertSame(response, resp.getBody());
        verify(mapper).toDomain(req);
        verify(createUC).execute(domain);
        verify(mapper).toResponse(created);
    }

    @Test
    void update_shouldReturnOk_withUpdatedBody() {
        Long id = 7L;
        PersonUpdateRequest req = mock(PersonUpdateRequest.class);
        Person domain = mock(Person.class);
        Person updated = mock(Person.class);
        PersonResponse response = mock(PersonResponse.class);

        when(req.name()).thenReturn("Updated Person");
        when(mapper.toDomain(req)).thenReturn(domain);
        when(updateUC.execute(id, domain)).thenReturn(updated);
        when(mapper.toResponse(updated)).thenReturn(response);

        ResponseEntity<PersonResponse> resp = controller.update(id, req);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(response, resp.getBody());
        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domain);
        verify(mapper).toResponse(updated);
    }

    @Test
    void getById_shouldReturnOk_withMappedBody() {
        Long id = 11L;
        Person person = mock(Person.class);
        PersonResponse response = mock(PersonResponse.class);

        when(getUC.execute(id)).thenReturn(person);
        when(mapper.toResponse(person)).thenReturn(response);

        ResponseEntity<PersonResponse> resp = controller.getById(id);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(response, resp.getBody());
        verify(getUC).execute(id);
        verify(mapper).toResponse(person);
    }

    @Test
    void delete_shouldCallUseCase_andReturnNoContent() {
        Long id = 99L;

        ResponseEntity<Void> resp = controller.delete(id);

        assertEquals(204, resp.getStatusCodeValue());
        assertNull(resp.getBody());
        verify(deleteUC).execute(id);
    }
}
