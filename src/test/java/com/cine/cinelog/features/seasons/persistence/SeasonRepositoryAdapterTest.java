package com.cine.cinelog.features.seasons.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import com.cine.cinelog.features.seasons.repository.SeasonJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonRepositoryAdapterTest {

    @Mock
    private SeasonJpaRepository jpa;

    @Mock
    private SeasonMapper seasonMapper;

    @InjectMocks
    private SeasonRepositoryAdapter adapter;

    @Test
    void save_should_map_entity_and_return_domain() {
        Season domain = mock(Season.class);
        SeasonEntity entity = mock(SeasonEntity.class);
        SeasonEntity savedEntity = mock(SeasonEntity.class);
        Season savedDomain = mock(Season.class);

        when(seasonMapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(seasonMapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Season result = adapter.save(domain);

        assertSame(savedDomain, result);
        verify(seasonMapper).toEntity(domain);
        verify(jpa).save(entity);
        verify(seasonMapper).toDomain(savedEntity);
    }

    @Test
    void findById_should_return_mapped_domain_when_present() {
        Long id = 1L;
        SeasonEntity entity = mock(SeasonEntity.class);
        Season domain = mock(Season.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(seasonMapper.toDomain(entity)).thenReturn(domain);

        Optional<Season> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(seasonMapper).toDomain(entity);
    }

    @Test
    void findById_should_return_empty_when_not_found() {
        Long id = 2L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<Season> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(seasonMapper);
    }

    @Test
    void findAll_should_build_pageable_call_jpa_and_map_entities() {
        PageQuery query = mock(PageQuery.class);
        when(query.page()).thenReturn(1);
        when(query.size()).thenReturn(2);
        when(query.direction()).thenReturn("ASC");
        when(query.sort()).thenReturn("name");

        SeasonEntity entity = mock(SeasonEntity.class);
        Season mapped = mock(Season.class);

        Page<SeasonEntity> page = new PageImpl<>(
                List.of(entity),
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "name")),
                1L);

        when(jpa.findAll(any(Pageable.class))).thenReturn(page);
        when(seasonMapper.toDomain(entity)).thenReturn(mapped);

        PageResult<Season> result = adapter.findAll(query);

        assertNotNull(result);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findAll(captor.capture());
        Pageable captured = captor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(2, captured.getPageSize());
        assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("name").getDirection());
        verify(seasonMapper).toDomain(entity);
    }

    @Test
    void deleteById_should_delegate_to_jpa() {
        Long id = 5L;
        adapter.deleteById(id);
        verify(jpa).deleteById(id);
    }
}