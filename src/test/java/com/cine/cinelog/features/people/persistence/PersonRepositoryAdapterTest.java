package com.cine.cinelog.features.people.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import com.cine.cinelog.features.people.repository.PersonJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @InjectMocks
    private PersonRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void save_shouldReturnMappedPerson() {
        Person domain = mock(Person.class);
        PersonEntity entity = mock(PersonEntity.class);
        PersonEntity savedEntity = mock(PersonEntity.class);
        Person savedDomain = mock(Person.class);

        when(personMapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(personMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Person result = adapter.save(domain);

        assertSame(savedDomain, result);
        verify(personMapper).toEntity(domain);
        verify(jpa).save(entity);
        verify(personMapper).toDomain(savedEntity);
    }

    @Test
    void findById_whenPresent_shouldReturnDomain() {
        Long id = 123L;
        PersonEntity entity = mock(PersonEntity.class);
        Person domain = mock(Person.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(personMapper.toDomain(entity)).thenReturn(domain);

        Optional<Person> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(personMapper).toDomain(entity);
    }

    @Test
    void findById_whenNotPresent_shouldReturnEmpty() {
        Long id = 456L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<Person> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(personMapper);
    }

    @Test
    void findAll_shouldUsePageQueryAndMapEntities() {
        PageQuery query = mock(PageQuery.class);
        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(2);
        when(query.sort()).thenReturn("name");
        when(query.direction()).thenReturn("ASC");

        PersonEntity e1 = mock(PersonEntity.class);
        PersonEntity e2 = mock(PersonEntity.class);
        Person p1 = mock(Person.class);
        Person p2 = mock(Person.class);

        Page<PersonEntity> page = new PageImpl<>(List.of(e1, e2));
        when(jpa.findAll(any(Pageable.class))).thenReturn(page);
        when(personMapper.toDomain(e1)).thenReturn(p1);
        when(personMapper.toDomain(e2)).thenReturn(p2);

        PageResult<Person> result = adapter.findAll(query);

        assertNotNull(result);
        verify(jpa).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(2, captured.getPageSize());
        Sort.Order order = captured.getSort().getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());

        // ensure mapper was used for each entity
        verify(personMapper).toDomain(e1);
        verify(personMapper).toDomain(e2);
    }

    @Test
    void deleteById_shouldDelegateToJpa() {
        Long id = 999L;
        doNothing().when(jpa).deleteById(id);

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
    }
}