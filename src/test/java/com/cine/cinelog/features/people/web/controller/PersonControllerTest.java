package com.cine.cinelog.features.people.web.controller;

import com.cine.cinelog.core.application.ports.in.person.CreatePersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.DeletePersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.GetPersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.ListPeopleUseCase;
import com.cine.cinelog.core.application.ports.in.person.UpdatePersonUseCase;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.web.dto.PersonCreateRequest;
import com.cine.cinelog.features.people.web.dto.PersonResponse;
import com.cine.cinelog.features.people.web.dto.PersonUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
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
    private PersonMapper mapper;

    @InjectMocks
    private PersonController controller;

    @Test
    void create_shouldReturnCreatedWithLocationAndBody() {
        PersonCreateRequest req = mock(PersonCreateRequest.class);
        Person domain = mock(Person.class);
        PersonResponse resp = mock(PersonResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(createUC.execute(domain)).thenReturn(domain);
        when(domain.getId()).thenReturn(1L);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<PersonResponse> result = controller.create(req);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());
        assertEquals(URI.create("/api/people/1"), result.getHeaders().getLocation());
    }

    @Test
    void update_shouldReturnOkWithBody() {
        PersonUpdateRequest req = mock(PersonUpdateRequest.class);
        Person domain = mock(Person.class);
        Person updated = mock(Person.class);
        PersonResponse resp = mock(PersonResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(updateUC.execute(2L, domain)).thenReturn(updated);
        when(mapper.toResponse(updated)).thenReturn(resp);

        ResponseEntity<PersonResponse> result = controller.update(2L, req);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());
    }

    @Test
    void getById_shouldReturnOkWithBody() {
        Person domain = mock(Person.class);
        PersonResponse resp = mock(PersonResponse.class);

        when(getUC.execute(3L)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(resp);

        ResponseEntity<PersonResponse> result = controller.getById(3L);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals(resp, result.getBody());
    }

    @Test
    void list_shouldReturnOkWithMappedList() {
        Person p1 = mock(Person.class);
        Person p2 = mock(Person.class);
        PersonResponse r1 = mock(PersonResponse.class);
        PersonResponse r2 = mock(PersonResponse.class);

        when(listUC.execute()).thenReturn(List.of(p1, p2));
        when(mapper.toResponse(p1)).thenReturn(r1);
        when(mapper.toResponse(p2)).thenReturn(r2);

        ResponseEntity<List<PersonResponse>> result = controller.list();

        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getBody());
        assertEquals(List.of(r1, r2), result.getBody());
    }

    @Test
    void delete_shouldInvokeUseCaseAndReturnNoContent() {
        ResponseEntity<Void> result = controller.delete(4L);

        assertEquals(204, result.getStatusCodeValue());
        verify(deleteUC).execute(4L);
    }
}