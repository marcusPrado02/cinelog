package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.PersonItem;
import com.cine.cinelog.features.reports.data.TopActorsData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the "Top Actors" report — actors with the most highly-rated films
 * in the CineLog catalog, with profile images and filmography highlights.
 */
@Service
public class TopActorsQueryService {

    private static final int DEFAULT_LIMIT = 10;
    private final JdbcTemplate jdbc;

    public TopActorsQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Queries actors ranked by average rating of their films (minimum 2 credits).
     */
    public TopActorsData build(int limit) {
        List<PersonItem> actors = jdbc.query(
                "SELECT p.id, p.name, p.profile_url, p.tmdb_person_id, "
                        + "COUNT(DISTINCT c.media_id) AS film_count, "
                        + "ROUND(AVG(mp.avg_rating), 2) AS avg_rating "
                        + "FROM people p "
                        + "JOIN credits c ON c.person_id = p.id AND c.role = 'ACTOR' "
                        + "JOIN media_popularity mp ON mp.media_id = c.media_id AND mp.ratings_count > 0 "
                        + "GROUP BY p.id, p.name, p.profile_url, p.tmdb_person_id "
                        + "HAVING COUNT(DISTINCT c.media_id) >= 2 "
                        + "ORDER BY avg_rating DESC, film_count DESC "
                        + "LIMIT ?",
                (rs, rn) -> new PersonItem(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("profile_url"),
                        rs.getObject("tmdb_person_id", Long.class),
                        rs.getInt("film_count"),
                        rs.getBigDecimal("avg_rating"),
                        "ACTOR"),
                limit);

        // Enrich each actor with their top 3 media
        for (PersonItem actor : actors) {
            List<MediaItem> topMedia = jdbc.query(
                    "SELECT m.id, m.title, m.type, m.poster_url, m.backdrop_url, "
                            + "mp.avg_rating, mp.watch_count, m.release_year "
                            + "FROM credits c "
                            + "JOIN media m ON m.id = c.media_id "
                            + "LEFT JOIN media_popularity mp ON mp.media_id = m.id "
                            + "WHERE c.person_id = ? AND c.role = 'ACTOR' "
                            + "ORDER BY COALESCE(mp.avg_rating, 0) DESC "
                            + "LIMIT 3",
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
                    actor.getId());
            actor.setTopMedia(topMedia);
        }

        return new TopActorsData(actors, LocalDateTime.now(), limit);
    }

    public TopActorsData build() {
        return build(DEFAULT_LIMIT);
    }
}
