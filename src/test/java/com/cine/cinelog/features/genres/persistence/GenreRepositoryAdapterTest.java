package com.cine.cinelog.features.genres.persistence;

import com.cine.cinelog.features.genres.mapper.GenreMapper;
import com.cine.cinelog.features.genres.persistence.entity.GenreEntity;
import com.cine.cinelog.features.genres.repository.GenreJpaRepository;
import com.cine.cinelog.core.domain.model.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GenreRepositoryAdapterTest {

    @Mock
    private GenreJpaRepository jpa; // raw mocked to avoid needing entity class

    @Mock
    private GenreMapper genreMapper; // raw mocked

    @InjectMocks
    private GenreRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        // Mockito annotations handled by extension and @InjectMocks
    }

    @Test
    void save_shouldMapToEntity_callSave_andMapToDomain() {
        Genre domainInput = mock(Genre.class);
        Genre domainOutput = mock(Genre.class);
        GenreEntity entity = mock(GenreEntity.class);

        when(genreMapper.toEntity(domainInput)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(genreMapper.toDomain(entity)).thenReturn(domainOutput);

        Genre result = adapter.save(domainInput);

        assertSame(domainOutput, result);
        verify(genreMapper).toEntity(domainInput);
        verify(jpa).save(entity);
        verify(genreMapper).toDomain(entity);
    }

    @Test
    void findById_whenPresent_shouldReturnMappedDomain() {
        long id = 42L;
        GenreEntity entity = mock(GenreEntity.class);
        Genre domain = mock(Genre.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(genreMapper.toDomain(entity)).thenReturn(domain);

        Optional<Genre> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(genreMapper).toDomain(entity);
    }

    @Test
    void findById_whenNotPresent_shouldReturnEmpty() {
        long id = 99L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<Genre> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(genreMapper);
    }

    @Test
    void findAll_shouldMapAllEntitiesToDomainList() {
        GenreEntity e1 = mock(GenreEntity.class);
        GenreEntity e2 = mock(GenreEntity.class);
        Genre g1 = mock(Genre.class);
        Genre g2 = mock(Genre.class);

        when(jpa.findAll()).thenReturn(asList(e1, e2));
        when(genreMapper.toDomain(e1)).thenReturn(g1);
        when(genreMapper.toDomain(e2)).thenReturn(g2);

        List<Genre> result = adapter.findAll();

        assertEquals(2, result.size());
        assertSame(g1, result.get(0));
        assertSame(g2, result.get(1));
        verify(jpa).findAll();
        verify(genreMapper).toDomain(e1);
        verify(genreMapper).toDomain(e2);
    }

    @Test
    void deleteById_shouldDelegateToJpa() {
        long id = 7L;
        doNothing().when(jpa).deleteById(id);

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
    }
}