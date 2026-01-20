package com.cine.cinelog.features.media.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.repository.MediaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRepositoryAdapterTest {

    @Mock
    private MediaJpaRepository repository;

    @Mock
    private MediaMapper mapper;

    @Mock
    private com.cine.cinelog.core.domain.model.Media domainMedia;

    @Mock
    private com.cine.cinelog.core.domain.model.Media mappedDomain;

    @Mock
    private MediaEntity entity;

    @Mock
    private MediaEntity savedEntity;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private MediaRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MediaRepositoryAdapter(repository, mapper);
    }

    @Test
    void save_shouldMapToEntity_callRepository_andMapToDomain() {
        when(mapper.toEntity(domainMedia)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mappedDomain);

        var result = adapter.save(domainMedia);

        assertSame(mappedDomain, result);
        verify(mapper).toEntity(domainMedia);
        verify(repository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findById_whenPresent_shouldReturnMappedDomain() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mappedDomain);

        var opt = adapter.findById(1L);

        assertTrue(opt.isPresent());
        assertSame(mappedDomain, opt.get());
        verify(repository).findById(1L);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_whenNotPresent_shouldReturnEmpty() {
        when(repository.findById(2L)).thenReturn(Optional.empty());

        var opt = adapter.findById(2L);

        assertFalse(opt.isPresent());
        verify(repository).findById(2L);
        verifyNoInteractions(mapper);
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        adapter.deleteById(5L);

        verify(repository).deleteById(5L);
        verifyNoInteractions(mapper);
    }

    @Test
    void listAll_shouldRequestPage_andMapEachEntity() {
        MediaEntity e1 = mock(MediaEntity.class);
        MediaEntity e2 = mock(MediaEntity.class);
        var page = new PageImpl<>(List.of(e1, e2));

        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        when(mapper.toDomain(e1)).thenReturn(mock(com.cine.cinelog.core.domain.model.Media.class));
        when(mapper.toDomain(e2)).thenReturn(mock(com.cine.cinelog.core.domain.model.Media.class));

        PageQuery pageQuery = mock(PageQuery.class);
        when(pageQuery.page()).thenReturn(1);
        when(pageQuery.size()).thenReturn(5);

        var result = adapter.listAll(pageQuery);

        assertNotNull(result);
        verify(repository).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());

        verify(mapper).toDomain(e1);
        verify(mapper).toDomain(e2);
    }

    @Test
    void search_shouldBuildSpec_usePageable_andMapResults() {
        MediaEntity e1 = mock(MediaEntity.class);
        var page = new PageImpl<>(List.of(e1));

        when(repository.findAll((Specification<MediaEntity>) any(),
                any(Pageable.class))).thenReturn(page);
        when(mapper.toDomain(e1)).thenReturn(mock(com.cine.cinelog.core.domain.model.Media.class));

        MediaSearchCriteria criteria = mock(MediaSearchCriteria.class);
        PageQuery pageQuery = mock(PageQuery.class);
        when(pageQuery.page()).thenReturn(0);
        when(pageQuery.size()).thenReturn(10);

        var result = adapter.search(criteria, pageQuery);

        assertNotNull(result);
        verify(repository).findAll((Specification<MediaEntity>) any(),
                pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());

        verify(mapper).toDomain(e1);
    }
}