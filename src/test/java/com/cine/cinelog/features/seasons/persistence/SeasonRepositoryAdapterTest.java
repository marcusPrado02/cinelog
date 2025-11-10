package com.cine.cinelog.features.seasons.persistence;

import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import com.cine.cinelog.features.seasons.repository.SeasonJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @BeforeEach
    void setUp() {
        // adapter is injected by Mockito via @InjectMocks
    }

    @Test
    void save_should_map_entity_save_and_return_domain() {
        Season domain = mock(Season.class);
        SeasonEntity entity = mock(SeasonEntity.class);

        when(seasonMapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(seasonMapper.toDomain(entity)).thenReturn(domain);

        Season result = adapter.save(domain);

        assertSame(domain, result);
        verify(seasonMapper).toEntity(domain);
        verify(jpa).save(entity);
        verify(seasonMapper).toDomain(entity);
    }

    @Test
    void findById_should_return_mapped_domain_when_present() {
        Long id = 1L;
        Season domain = mock(Season.class);
        SeasonEntity entity = mock(SeasonEntity.class);

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
    void findAll_should_map_all_entities_to_domains() {
        Season domain = mock(Season.class);
        SeasonEntity entity = mock(SeasonEntity.class);

        when(jpa.findAll()).thenReturn(List.of(entity));
        when(seasonMapper.toDomain(entity)).thenReturn(domain);

        List<Season> result = adapter.findAll();

        assertEquals(1, result.size());
        assertSame(domain, result.get(0));
        verify(jpa).findAll();
        verify(seasonMapper).toDomain(entity);
    }

    @Test
    void deleteById_should_delegate_to_jpa() {
        Long id = 3L;

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
        verifyNoMoreInteractions(jpa);
        verifyNoInteractions(seasonMapper);
    }
}