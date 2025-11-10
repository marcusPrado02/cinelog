package com.cine.cinelog.features.media.persistence;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.repository.MediaJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRepositoryAdapterTest {

    @Mock
    private MediaJpaRepository repository;

    @Mock
    private MediaMapper mapper;

    @InjectMocks
    private MediaRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void save_shouldMapEntityAndReturnDomain() {
        Media domain = mock(Media.class);
        MediaEntity entity = mock(MediaEntity.class);

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Media result = adapter.save(domain);

        assertSame(domain, result);
        verify(mapper).toEntity(domain);
        verify(repository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_whenPresent_shouldReturnDomain() {
        Long id = 1L;
        MediaEntity entity = mock(MediaEntity.class);
        Media domain = mock(Media.class);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Media> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(repository).findById(id);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_whenAbsent_shouldReturnEmpty() {
        Long id = 2L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<Media> result = adapter.findById(id);

        assertTrue(result.isEmpty());
        verify(repository).findById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        Long id = 3L;

        adapter.deleteById(id);

        verify(repository).deleteById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void find_withTypeAndQuery_shouldUseFindByTypeAndTitleContainingIgnoreCase() {
        MediaType type = MediaType.MOVIE;
        String q = "Star";
        int page = 0;
        int size = 5;

        MediaEntity e1 = mock(MediaEntity.class);
        MediaEntity e2 = mock(MediaEntity.class);
        Media m1 = mock(Media.class);
        Media m2 = mock(Media.class);

        when(repository.findByTypeAndTitleContainingIgnoreCase(eq(type), eq(q), any(Pageable.class)))
                .thenReturn(new PageImpl<>(asList(e1, e2)));
        when(mapper.toDomain(e1)).thenReturn(m1);
        when(mapper.toDomain(e2)).thenReturn(m2);

        List<Media> result = adapter.find(type, q, page, size);

        assertEquals(2, result.size());
        assertSame(m1, result.get(0));
        assertSame(m2, result.get(1));
        verify(repository).findByTypeAndTitleContainingIgnoreCase(eq(type), eq(q), any(Pageable.class));
        verify(mapper, times(2)).toDomain(any(MediaEntity.class));
    }

    @Test
    void find_withOnlyType_shouldUseFindByType() {
        MediaType type = MediaType.SERIES;
        String q = null;
        int page = 1;
        int size = 3;

        MediaEntity e = mock(MediaEntity.class);
        Media m = mock(Media.class);

        when(repository.findByType(eq(type), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(e)));
        when(mapper.toDomain(e)).thenReturn(m);

        List<Media> result = adapter.find(type, q, page, size);

        assertEquals(1, result.size());
        assertSame(m, result.get(0));
        verify(repository).findByType(eq(type), any(Pageable.class));
    }

    @Test
    void find_withOnlyQuery_shouldUseFindByTitleContainingIgnoreCase() {
        MediaType type = null;
        String q = "Hero";
        int page = 2;
        int size = 4;

        MediaEntity e = mock(MediaEntity.class);
        Media m = mock(Media.class);

        when(repository.findByTitleContainingIgnoreCase(eq(q), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e)));
        when(mapper.toDomain(e)).thenReturn(m);

        List<Media> result = adapter.find(type, q, page, size);

        assertEquals(1, result.size());
        assertSame(m, result.get(0));
        verify(repository).findByTitleContainingIgnoreCase(eq(q), any(Pageable.class));
    }

    @Test
    void find_withNoTypeAndNoQuery_shouldUseFindAll_and_adjustPageableBounds() {
        MediaType type = null;
        String q = null;
        int page = -5;
        int size = 0;

        MediaEntity e = mock(MediaEntity.class);
        Media m = mock(Media.class);

        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(e)));
        when(mapper.toDomain(e)).thenReturn(m);

        List<Media> result = adapter.find(type, q, page, size);

        assertEquals(1, result.size());
        assertSame(m, result.get(0));
        verify(repository).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber()); // page should be max(page, 0)
        assertEquals(1, captured.getPageSize()); // size should be max(size, 1)
    }
}