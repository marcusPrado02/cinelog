package com.cine.cinelog.features.credits.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import com.cine.cinelog.features.credits.repository.CreditJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditRepositoryAdapterTest {

    @Mock
    private CreditJpaRepository jpa;

    @Mock
    private CreditMapper creditMapper;

    @InjectMocks
    private CreditRepositoryAdapter adapter;

    @Mock
    private PageQuery pageQuery;

    @Test
    void save_should_map_entity_and_return_domain() {
        var domain = mock(com.cine.cinelog.core.domain.model.Credit.class);
        var entity = mock(CreditEntity.class);
        var savedEntity = mock(CreditEntity.class);
        var savedDomain = mock(com.cine.cinelog.core.domain.model.Credit.class);

        when(creditMapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(creditMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        var result = adapter.save(domain);

        assertSame(savedDomain, result);
        verify(creditMapper).toEntity(domain);
        verify(jpa).save(entity);
        verify(creditMapper).toDomain(savedEntity);
    }

    @Test
    void findById_when_present_should_map_to_domain() {
        var id = 1L;
        var entity = mock(CreditEntity.class);
        var domain = mock(com.cine.cinelog.core.domain.model.Credit.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(creditMapper.toDomain(entity)).thenReturn(domain);

        var result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(creditMapper).toDomain(entity);
    }

    @Test
    void findById_when_not_present_should_return_empty() {
        var id = 2L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        var result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(creditMapper);
    }

    @Test
    void findAll_should_build_pageable_and_map_entities() {
        when(pageQuery.page()).thenReturn(2);
        when(pageQuery.size()).thenReturn(5);
        when(pageQuery.direction()).thenReturn("ASC");
        when(pageQuery.sort()).thenReturn("name");

        var entity = mock(CreditEntity.class);
        var domain = mock(com.cine.cinelog.core.domain.model.Credit.class);

        when(jpa.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(creditMapper.toDomain(entity)).thenReturn(domain);

        var result = adapter.findAll(pageQuery);

        // verify pageable passed to jpa
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(2, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());
        assertNotNull(captured.getSort());
        assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("name").getDirection());

        // verify mapper used for each entity
        verify(creditMapper).toDomain(entity);

        assertNotNull(result);
    }

    @Test
    void deleteById_should_delegate_to_jpa() {
        var id = 33L;
        doNothing().when(jpa).deleteById(id);

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
    }
}