package com.cine.cinelog.features.genres.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaGenreJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private MediaGenreJdbcRepository repository;

    @Test
    void link_shouldCallJdbcUpdateWithInsertSqlAndParameters() {
        long mediaId = 123L;
        long genreId = 5L;

        repository.link(mediaId, genreId);

        String expectedSql = "INSERT INTO media_genres(media_id, genre_id) VALUES (?, ?)";
        verify(jdbc, times(1)).update(expectedSql, mediaId, genreId);
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void unlink_shouldCallJdbcUpdateWithDeleteSqlAndParameters() {
        long mediaId = 987L;
        long genreId = 2L;

        repository.unlink(mediaId, genreId);

        String expectedSql = "DELETE FROM media_genres WHERE media_id = ? AND genre_id = ?";
        verify(jdbc, times(1)).update(expectedSql, mediaId, genreId);
        verifyNoMoreInteractions(jdbc);
    }
}
