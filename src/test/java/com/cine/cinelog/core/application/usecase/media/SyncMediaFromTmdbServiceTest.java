package com.cine.cinelog.core.application.usecase.media;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.application.ports.out.MediaGenreLinkPort;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.TmdbClientPort;

@ExtendWith(MockitoExtension.class)
class SyncMediaFromTmdbServiceTest {

    @Mock
    private MediaRepositoryPort mediaRepository;

    @Mock
    private TmdbClientPort tmdbClient;

    @Mock
    private GenreRepositoryPort genreRepository;

    @Mock
    private MediaGenreLinkPort mediaGenreLinkPort;

    @InjectMocks
    private SyncMediaFromTmdbService service;

    @Test
    void sync_whenMediaNotFound_throwsDomainException() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () -> service.sync(1L));
        assertTrue(ex.getMessage().contains("Media not found"));
    }

    @Test
    void sync_withTmdbId_appliesDetailsSavesMediaAndReplacesGenres() {
        // Prepare local media with existing tmdbId
        Media media = new Media();
        media.setId(1L);
        media.setTmdbId(100L);
        media.setTitle("Old Title");
        media.setReleaseYear(2000);

        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

        // Prepare tmdb details mock
        TmdbMediaDetails tmdbDetails = mock(TmdbMediaDetails.class);
        when(tmdbClient.fetchByTmdbId(100L)).thenReturn(Optional.of(tmdbDetails));

        when(tmdbDetails.getTmdbId()).thenReturn(100L);
        when(tmdbDetails.getTitle()).thenReturn("New Title");
        when(tmdbDetails.getOriginalTitle()).thenReturn("Orig Title");
        when(tmdbDetails.getOverview()).thenReturn("Overview");
        when(tmdbDetails.getReleaseYear()).thenReturn(2021);
        when(tmdbDetails.getPosterUrl()).thenReturn("poster.jpg");
        when(tmdbDetails.getBackdropUrl()).thenReturn("backdrop.jpg");
        when(tmdbDetails.getOriginalLanguage()).thenReturn("en");
        when(tmdbDetails.getType()).thenReturn(null); // type optional
        when(tmdbDetails.getGenres()).thenReturn(List.of("Action", "Drama", "Action", "   "));

        // genreRepository: none exist initially
        when(genreRepository.findByName(eq("Action"))).thenReturn(Optional.empty());
        when(genreRepository.findByName(eq("Drama"))).thenReturn(Optional.empty());

        // simulate save assigning ids based on name
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
            Genre g = invocation.getArgument(0);
            if ("Action".equals(g.getName())) {
                g.setId(10L);
            } else if ("Drama".equals(g.getName())) {
                g.setId(20L);
            } else {
                g.setId(99L);
            }
            return g;
        });

        // Execute
        Media result = service.sync(1L);

        // Verify media fields updated and saved
        assertEquals(100L, result.getTmdbId());
        assertEquals("New Title", result.getTitle());
        assertEquals("Orig Title", result.getOriginalTitle());
        assertEquals("Overview", result.getOverview());
        assertEquals(2021, result.getReleaseYear());
        verify(mediaRepository).save(media);

        // Verify genres replaced with expected ids (Action -> 10, Drama -> 20) in order
        // of first appearance
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(mediaGenreLinkPort).replaceGenres(eq(media.getId()), captor.capture());
        List<Long> replaced = captor.getValue();
        assertEquals(2, replaced.size());
        assertEquals(10L, replaced.get(0));
        assertEquals(20L, replaced.get(1));
    }

    @Test
    void sync_whenTmdbReturnsNoGenres_doesNotCallReplaceGenres() {
        Media media = new Media();
        media.setId(2L);
        media.setTitle("Some Movie");
        media.setReleaseYear(1999);
        // no tmdbId -> fallback to search
        when(mediaRepository.findById(2L)).thenReturn(Optional.of(media));

        TmdbMediaDetails tmdbDetails = mock(TmdbMediaDetails.class);
        when(tmdbClient.searchByTitleAndYear("Some Movie", 1999)).thenReturn(Optional.of(tmdbDetails));
        when(tmdbDetails.getGenres()).thenReturn(null); // no genres provided

        service.sync(2L);

        verify(mediaGenreLinkPort, never()).replaceGenres(anyLong(), anyList());
        verify(mediaRepository).save(media);
    }
}