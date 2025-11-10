package com.cine.cinelog.features.credits.persistence;

import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import com.cine.cinelog.features.credits.repository.CreditJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditRepositoryAdapterTest {

    @Mock
    private CreditJpaRepository jpa;

    @Mock
    private CreditMapper creditMapper;

    @InjectMocks
    private CreditRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSaveCredit() {
        Credit domain = mock(Credit.class);
        CreditEntity entity = mock(CreditEntity.class);

        when(creditMapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(creditMapper.toDomain(entity)).thenReturn(domain);

        Credit result = adapter.save(domain);

        assertSame(domain, result);
        verify(creditMapper).toEntity(domain);
        verify(jpa).save(entity);
        verify(creditMapper).toDomain(entity);
    }

    @Test
    void shouldFindByIdWhenPresent() {
        long id = 1L;
        CreditEntity entity = mock(CreditEntity.class);
        Credit domain = mock(Credit.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(creditMapper.toDomain(entity)).thenReturn(domain);

        Optional<Credit> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(creditMapper).toDomain(entity);
    }

    @Test
    void shouldReturnEmptyWhenFindByIdNotFound() {
        long id = 2L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<Credit> result = adapter.findById(id);

        assertTrue(result.isEmpty());
        verify(jpa).findById(id);
        verifyNoInteractions(creditMapper);
    }

    @Test
    void shouldFindAllAndMapToDomain() {
        CreditEntity e1 = mock(CreditEntity.class);
        CreditEntity e2 = mock(CreditEntity.class);
        Credit d1 = mock(Credit.class);
        Credit d2 = mock(Credit.class);

        when(jpa.findAll()).thenReturn(List.of(e1, e2));
        when(creditMapper.toDomain(e1)).thenReturn(d1);
        when(creditMapper.toDomain(e2)).thenReturn(d2);

        List<Credit> result = adapter.findAll();

        assertEquals(2, result.size());
        assertSame(d1, result.get(0));
        assertSame(d2, result.get(1));
        verify(jpa).findAll();
        verify(creditMapper).toDomain(e1);
        verify(creditMapper).toDomain(e2);
    }

    @Test
    void shouldDeleteById() {
        long id = 3L;

        doNothing().when(jpa).deleteById(id);

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
    }
}