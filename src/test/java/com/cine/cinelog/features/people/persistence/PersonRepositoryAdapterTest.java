package com.cine.cinelog.features.people.persistence;

import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.repository.PersonJpaRepository;
import com.cine.cinelog.features.people.web.dto.PersonCreateRequest;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonRepositoryAdapterTest {

    @Mock
    private PersonJpaRepository jpa;

    @Mock
    private PersonMapper personMapper;

    private PersonRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PersonRepositoryAdapter(jpa, personMapper);
    }

    @Test
    void save_shouldMapToEntityCallSaveAndMapToDomain() {
        Person input = mock(Person.class);
        Person savedDomain = mock(Person.class);

        when(personMapper.toEntity(input)).thenReturn(null);
        when(jpa.save(ArgumentMatchers.isNull())).thenReturn(null);
        when(personMapper.toDomain(ArgumentMatchers.<PersonCreateRequest>isNull())).thenReturn(savedDomain);

        Person result = adapter.save(input);

        assertSame(savedDomain, result);
        verify(personMapper).toEntity(input);
        verify(jpa).save(null);
        verify(personMapper).toDomain((PersonCreateRequest) null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void findById_whenPresent_shouldReturnMappedDomain() {
        Object entity = new Object();
        Person expected = mock(Person.class);

        when((Optional) jpa.findById(1L)).thenReturn(Optional.of(entity));
        when(personMapper.toDomain(ArgumentMatchers.<PersonCreateRequest>any())).thenReturn(expected);

        Optional<Person> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        verify(jpa).findById(1L);
        verify(personMapper).toDomain(ArgumentMatchers.<PersonCreateRequest>any());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void findById_whenNotPresent_shouldReturnEmpty() {
        when((Optional) jpa.findById(2L)).thenReturn(Optional.empty());

        Optional<Person> result = adapter.findById(2L);

        assertFalse(result.isPresent());
        verify(jpa).findById(2L);
        verifyNoInteractions(personMapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void findAll_shouldMapAllEntitiesToDomainList() {
        Object e1 = new Object();
        Object e2 = new Object();
        Person p1 = mock(Person.class);
        Person p2 = mock(Person.class);

        when((List) jpa.findAll()).thenReturn(List.of(e1, e2));
        when(personMapper.toDomain(ArgumentMatchers.<PersonCreateRequest>any())).thenReturn(p1, p2);

        List<Person> result = adapter.findAll();

        assertEquals(2, result.size());
        assertSame(p1, result.get(0));
        assertSame(p2, result.get(1));
        verify(jpa).findAll();
        verify(personMapper, times(2)).toDomain(ArgumentMatchers.<PersonCreateRequest>any());
    }

    @Test
    void deleteById_shouldDelegateToJpa() {
        adapter.deleteById(3L);
        verify(jpa).deleteById(3L);
    }
}