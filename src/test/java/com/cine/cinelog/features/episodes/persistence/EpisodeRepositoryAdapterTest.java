package com.cine.cinelog.features.episodes.persistence;

import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import com.cine.cinelog.features.episodes.repository.EpisodeJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodeRepositoryAdapterTest {

    @Mock
    private EpisodeJpaRepository jpa;

    @Mock
    private EpisodeMapper episodeMapper;

    private EpisodeRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EpisodeRepositoryAdapter(jpa, episodeMapper);
    }

    @Test
    void save_shouldMapToEntitySaveAndMapToDomain() {
        Episode domainInput = mock(Episode.class);
        Episode domainSaved = mock(Episode.class);

        EpisodeEntity entityInput = new EpisodeEntity();
        EpisodeEntity entitySaved = new EpisodeEntity();

        when(episodeMapper.toEntity(domainInput)).thenReturn(entityInput);
        when(jpa.save(entityInput)).thenReturn(entitySaved);
        when(episodeMapper.toDomain(entitySaved)).thenReturn(domainSaved);

        Episode result = adapter.save(domainInput);

        assertSame(domainSaved, result);
        verify(episodeMapper).toEntity(domainInput);
        verify(jpa).save(entityInput);
        verify(episodeMapper).toDomain(entitySaved);
    }

    @Test
    void findById_whenPresent_shouldReturnMappedDomain() {
        Long id = 1L;
        EpisodeEntity entity = new EpisodeEntity();
        Episode domain = mock(Episode.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(episodeMapper.toDomain(entity)).thenReturn(domain);

        Optional<Episode> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(episodeMapper).toDomain(entity);
    }

    @Test
    void findById_whenNotPresent_shouldReturnEmpty() {
        Long id = 2L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<Episode> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(episodeMapper);
    }

    @Test
    void findAll_shouldMapAllEntitiesToDomainList() {
        EpisodeEntity e1 = new EpisodeEntity();
        EpisodeEntity e2 = new EpisodeEntity();
        Episode d1 = mock(Episode.class);
        Episode d2 = mock(Episode.class);

        when(jpa.findAll()).thenReturn(List.of(e1, e2));
        when(episodeMapper.toDomain(e1)).thenReturn(d1);
        when(episodeMapper.toDomain(e2)).thenReturn(d2);

        List<Episode> result = adapter.findAll();

        assertEquals(2, result.size());
        assertSame(d1, result.get(0));
        assertSame(d2, result.get(1));
        verify(jpa).findAll();
        verify(episodeMapper).toDomain(e1);
        verify(episodeMapper).toDomain(e2);
    }

    @Test
    void deleteById_shouldDelegateToJpa() {
        Long id = 3L;

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
        verifyNoMoreInteractions(jpa);
        verifyNoInteractions(episodeMapper);
    }
}