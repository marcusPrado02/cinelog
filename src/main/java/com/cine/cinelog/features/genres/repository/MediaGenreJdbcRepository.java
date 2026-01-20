package com.cine.cinelog.features.genres.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cine.cinelog.core.application.ports.out.MediaGenreLinkPort;

/**
 * Implementação JDBC da porta MediaGenreLinkPort.
 *
 * Responsável por manter os registros da tabela media_genres.
 */
@Repository
public class MediaGenreJdbcRepository implements MediaGenreLinkPort {

    private final JdbcTemplate jdbc;

    public MediaGenreJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void replaceGenres(long mediaId, Collection<Long> genreIds) {
        // Remove todos os vínculos atuais
        jdbc.update("DELETE FROM media_genres WHERE media_id = ?", mediaId);

        if (genreIds == null || genreIds.isEmpty()) {
            return;
        }

        // Insere os novos vínculos em batch
        jdbc.batchUpdate(
                "INSERT INTO media_genres(media_id, genre_id) VALUES (?, ?)",
                new BatchPreparedStatementSetter() {

                    private final Short[] ids = genreIds.toArray(new Short[0]);

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, mediaId);
                        ps.setShort(2, ids[i]);
                    }

                    @Override
                    public int getBatchSize() {
                        return ids.length;
                    }
                });
    }

    @Override
    public void link(long mediaId, long genreId) {
        jdbc.update("INSERT INTO media_genres(media_id, genre_id) VALUES (?, ?)", mediaId, genreId);
    }

    @Override
    public void unlink(long mediaId, long genreId) {
        jdbc.update("DELETE FROM media_genres WHERE media_id = ? AND genre_id = ?", mediaId, genreId);
    }
}
