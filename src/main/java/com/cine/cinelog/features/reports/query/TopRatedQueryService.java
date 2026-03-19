package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.TopRatedData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Queries the top-rated media based on the media_popularity read-model.
 *
 * <p>
 * Requires at least one rating to be included in the results.
 * </p>
 */
@Service
public class TopRatedQueryService {

    private static final int DEFAULT_LIMIT = 10;
    private final JdbcTemplate jdbc;

    public TopRatedQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the top N media items ordered by average rating.
     *
     * @param limit maximum number of items to return
     * @return populated {@link TopRatedData}
     */
    public TopRatedData build(int limit) {
        List<MediaItem> items = jdbc.query(
                "SELECT m.id, m.title, m.type, m.poster_url, m.backdrop_url, "
                        + "mp.avg_rating, mp.watch_count, m.release_year "
                        + "FROM media_popularity mp "
                        + "JOIN media m ON m.id = mp.media_id "
                        + "WHERE mp.ratings_count > 0 "
                        + "ORDER BY mp.avg_rating DESC, mp.ratings_count DESC "
                        + "LIMIT ?",
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
                limit);

        return new TopRatedData(items, LocalDateTime.now(), limit);
    }

    public TopRatedData build() {
        return build(DEFAULT_LIMIT);
    }
}
