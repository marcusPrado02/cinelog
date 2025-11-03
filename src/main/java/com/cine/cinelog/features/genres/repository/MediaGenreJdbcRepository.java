package com.cine.cinelog.features.genres.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MediaGenreJdbcRepository {
    private final JdbcTemplate jdbc;

    public MediaGenreJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void link(long mediaId, short genreId) {
        jdbc.update("INSERT IGNORE INTO media_genres(media_id, genre_id) VALUES (?,?)", mediaId, genreId);
    }

    public void unlink(long mediaId, short genreId) {
        jdbc.update("DELETE FROM media_genres WHERE media_id=? AND genre_id=?", mediaId, genreId);
    }
}