package com.cine.cinelog.features.episodes.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import com.cine.cinelog.features.episodes.repository.EpisodeJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
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
class EpisodeRepositoryAdapterTest {

    @Test
    void save_should_map_entity_and_return_domain() {
        EpisodeJpaRepository jpa = mock(EpisodeJpaRepository.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        EpisodeRepositoryAdapter adapter = new EpisodeRepositoryAdapter(jpa, mapper);

        Episode domain = mock(Episode.class);
        EpisodeEntity entity = mock(EpisodeEntity.class);
        EpisodeEntity savedEntity = mock(EpisodeEntity.class);
        Episode returnedDomain = mock(Episode.class);

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(returnedDomain);

        Episode result = adapter.save(domain);

        assertSame(returnedDomain, result);
        verify(mapper).toEntity(domain);
        verify(jpa).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findById_should_return_domain_when_found() {
        EpisodeJpaRepository jpa = mock(EpisodeJpaRepository.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        EpisodeRepositoryAdapter adapter = new EpisodeRepositoryAdapter(jpa, mapper);

        Long id = 42L;
        EpisodeEntity entity = mock(EpisodeEntity.class);
        Episode domain = mock(Episode.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Episode> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_should_return_empty_when_not_found() {
        EpisodeJpaRepository jpa = mock(EpisodeJpaRepository.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        EpisodeRepositoryAdapter adapter = new EpisodeRepositoryAdapter(jpa, mapper);

        Long id = 100L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<Episode> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void findAll_should_build_pageable_and_delegate_and_return_mapped_page_result() {
        EpisodeJpaRepository jpa = mock(EpisodeJpaRepository.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        EpisodeRepositoryAdapter adapter = new EpisodeRepositoryAdapter(jpa, mapper);

        PageQuery query = mock(PageQuery.class);
        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(10);
        when(query.direction()).thenReturn("ASC");
        when(query.sort()).thenReturn("id");

        EpisodeEntity entity = mock(EpisodeEntity.class);
        Page<EpisodeEntity> page = new PageImpl<>(List.of(entity));

        when(jpa.findAll(any(Pageable.class))).thenReturn(page);

        @SuppressWarnings("unchecked")
        PageResult<Episode> expected = (PageResult<Episode>) mock(PageResult.class);

        try (MockedStatic<PageResultMapper> mocked = mockStatic(PageResultMapper.class)) {
            mocked.when(() -> PageResultMapper.from(eq(page), any())).thenReturn(expected);

            PageResult<Episode> result = adapter.findAll(query);

            assertSame(expected, result);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(jpa).findAll(pageableCaptor.capture());
            Pageable captured = pageableCaptor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            Sort.Order order = captured.getSort().getOrderFor("id");
            assertNotNull(order);
            assertEquals(Sort.Direction.ASC, order.getDirection());

            mocked.verify(() -> PageResultMapper.from(eq(page), any()));
        }
    }

    @Test
    void deleteById_should_delegate_to_jpa() {
        EpisodeJpaRepository jpa = mock(EpisodeJpaRepository.class);
        EpisodeMapper mapper = mock(EpisodeMapper.class);
        EpisodeRepositoryAdapter adapter = new EpisodeRepositoryAdapter(jpa, mapper);

        Long id = 7L;
        adapter.deleteById(id);

        verify(jpa).deleteById(id);
        verifyNoMoreInteractions(jpa);
        verifyNoInteractions(mapper);
    }
}
