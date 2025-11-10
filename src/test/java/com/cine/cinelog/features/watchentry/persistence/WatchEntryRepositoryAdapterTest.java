package com.cine.cinelog.features.watchentry.persistence;

import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.features.watchentry.repository.WatchEntryJpaRepository;
import com.cine.cinelog.features.watchentry.mapper.WatchEntryMapper;
import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WatchEntryRepositoryAdapterTest {

    @Test
    void save_should_map_entity_and_return_domain() {
        WatchEntryJpaRepository jpa = mock(WatchEntryJpaRepository.class);
        WatchEntryMapper mapper = mock(WatchEntryMapper.class);
        WatchEntryRepositoryAdapter adapter = new WatchEntryRepositoryAdapter(jpa, mapper);

        WatchEntry domainIn = mock(WatchEntry.class);
        WatchEntryEntity entity = mock(WatchEntryEntity.class);
        WatchEntryEntity savedEntity = mock(WatchEntryEntity.class);
        WatchEntry domainOut = mock(WatchEntry.class);

        when(mapper.toEntity(domainIn)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(domainOut);

        WatchEntry result = adapter.save(domainIn);

        assertSame(domainOut, result);
        verify(mapper).toEntity(domainIn);
        verify(jpa).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findById_when_present_should_return_mapped_domain() {
        WatchEntryJpaRepository jpa = mock(WatchEntryJpaRepository.class);
        WatchEntryMapper mapper = mock(WatchEntryMapper.class);
        WatchEntryRepositoryAdapter adapter = new WatchEntryRepositoryAdapter(jpa, mapper);

        Long id = 42L;
        WatchEntryEntity entity = mock(WatchEntryEntity.class);
        WatchEntry domain = mock(WatchEntry.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<WatchEntry> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_when_absent_should_return_empty() {
        WatchEntryJpaRepository jpa = mock(WatchEntryJpaRepository.class);
        WatchEntryMapper mapper = mock(WatchEntryMapper.class);
        WatchEntryRepositoryAdapter adapter = new WatchEntryRepositoryAdapter(jpa, mapper);

        Long id = 99L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<WatchEntry> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void deleteById_should_delegate_to_jpa() {
        WatchEntryJpaRepository jpa = mock(WatchEntryJpaRepository.class);
        WatchEntryMapper mapper = mock(WatchEntryMapper.class);
        WatchEntryRepositoryAdapter adapter = new WatchEntryRepositoryAdapter(jpa, mapper);

        Long id = 7L;
        adapter.deleteById(id);

        verify(jpa).deleteById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void listByUser_should_call_search_and_map_page() {
        WatchEntryJpaRepository jpa = mock(WatchEntryJpaRepository.class);
        WatchEntryMapper mapper = mock(WatchEntryMapper.class);
        WatchEntryRepositoryAdapter adapter = new WatchEntryRepositoryAdapter(jpa, mapper);

        Long userId = 1L;
        Long mediaId = 2L;
        Long episodeId = 3L;
        Integer minRating = 4;
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);
        Pageable pageable = mock(Pageable.class);

        @SuppressWarnings("unchecked")
        Page<WatchEntryEntity> pageEntities = mock(Page.class);
        @SuppressWarnings("unchecked")
        Page<WatchEntry> pageDomains = mock(Page.class);

        when(jpa.search(userId, mediaId, episodeId, minRating, from, to, pageable)).thenReturn(pageEntities);
        when(pageEntities.map(ArgumentMatchers.<Function<WatchEntryEntity, WatchEntry>>any())).thenReturn(pageDomains);

        Page<WatchEntry> result = adapter.listByUser(userId, mediaId, episodeId, minRating, from, to, pageable);

        assertSame(pageDomains, result);
        verify(jpa).search(userId, mediaId, episodeId, minRating, from, to, pageable);
        verify(pageEntities).map(any());
    }
}