package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.GenreSpotlightData;
import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.PersonItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the "Genre Spotlight" report — deep dive into a specific genre
 * with stats, top media and top actors.
 */
@Service
public class GenreSpotlightQueryService {

    private final JdbcTemplate jdbc;

    public GenreSpotlightQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Builds a spotlight on a specific genre. If genreName is null,
     * auto-selects the genre with most media.
     */
    public GenreSpotlightData build(String genreName) {
        if (genreName == null || genreName.isBlank()) {
            genreName = jdbc.queryForObject(
                    "SELECT g.name FROM genres g "
                            + "JOIN media_genres mg ON mg.genre_id = g.id "
                            + "GROUP BY g.id, g.name ORDER BY COUNT(*) DESC LIMIT 1",
                    String.class);
        }

        final String genre = genreName;
        GenreSpotlightData data = new GenreSpotlightData();
        data.setGenreName(genre);
        data.setGeneratedAt(LocalDateTime.now());

        // counts
        Long totalMedia = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT mg.media_id) FROM media_genres mg "
                        + "JOIN genres g ON g.id = mg.genre_id WHERE g.name = ?",
                Long.class, genre);
        data.setTotalMedia(totalMedia != null ? totalMedia : 0);

        Long totalMovies = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT mg.media_id) FROM media_genres mg "
                        + "JOIN genres g ON g.id = mg.genre_id "
                        + "JOIN media m ON m.id = mg.media_id "
                        + "WHERE g.name = ? AND m.type = 'MOVIE'",
                Long.class, genre);
        data.setTotalMovies(totalMovies != null ? totalMovies : 0);

        Long totalSeries = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT mg.media_id) FROM media_genres mg "
                        + "JOIN genres g ON g.id = mg.genre_id "
                        + "JOIN media m ON m.id = mg.media_id "
                        + "WHERE g.name = ? AND m.type = 'SERIES'",
                Long.class, genre);
        data.setTotalSeries(totalSeries != null ? totalSeries : 0);

        // avg rating in genre
        BigDecimal avgRating = jdbc.query(
                "SELECT ROUND(AVG(mp.avg_rating), 2) FROM media_genres mg "
                        + "JOIN genres g ON g.id = mg.genre_id "
                        + "JOIN media_popularity mp ON mp.media_id = mg.media_id AND mp.ratings_count > 0 "
                        + "WHERE g.name = ?",
                rs -> rs.next() ? rs.getBigDecimal(1) : null,
                genre);
        data.setAvgRating(avgRating);

        // top 5 media in genre
        List<MediaItem> topMedia = jdbc.query(
                "SELECT m.id, m.title, m.type, m.poster_url, m.backdrop_url, "
                        + "mp.avg_rating, mp.watch_count, m.release_year "
                        + "FROM media_genres mg "
                        + "JOIN genres g ON g.id = mg.genre_id "
                        + "JOIN media m ON m.id = mg.media_id "
                        + "LEFT JOIN media_popularity mp ON mp.media_id = m.id "
                        + "WHERE g.name = ? AND mp.ratings_count > 0 "
                        + "ORDER BY mp.avg_rating DESC "
                        + "LIMIT 5",
                (rs, rn) -> {
                    MediaItem mi = new MediaItem(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("type"),
                            rs.getString("poster_url"),
                            rs.getBigDecimal("avg_rating"),
                            rs.getObject("watch_count", Long.class),
                            rs.getObject("release_year", Integer.class));
                    mi.setBackdropUrl(rs.getString("backdrop_url"));
                    return mi;
                },
                genre);
        data.setTopRatedInGenre(topMedia);

        // top 5 actors in genre
        List<PersonItem> topActors = jdbc.query(
                "SELECT p.id, p.name, p.profile_url, p.tmdb_person_id, "
                        + "COUNT(DISTINCT c.media_id) AS film_count, "
                        + "ROUND(AVG(mp.avg_rating), 2) AS avg_rating "
                        + "FROM media_genres mg "
                        + "JOIN genres g ON g.id = mg.genre_id "
                        + "JOIN credits c ON c.media_id = mg.media_id AND c.role = 'ACTOR' "
                        + "JOIN people p ON p.id = c.person_id "
                        + "LEFT JOIN media_popularity mp ON mp.media_id = c.media_id AND mp.ratings_count > 0 "
                        + "WHERE g.name = ? "
                        + "GROUP BY p.id, p.name, p.profile_url, p.tmdb_person_id "
                        + "HAVING COUNT(DISTINCT c.media_id) >= 2 "
                        + "ORDER BY avg_rating DESC, film_count DESC "
                        + "LIMIT 5",
                (rs, rn) -> new PersonItem(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("profile_url"),
                        rs.getObject("tmdb_person_id", Long.class),
                        rs.getInt("film_count"),
                        rs.getBigDecimal("avg_rating"),
                        "ACTOR"),
                genre);
        data.setTopActorsInGenre(topActors);

        return data;
    }

    public GenreSpotlightData build() {
        return build(null);
    }
}
