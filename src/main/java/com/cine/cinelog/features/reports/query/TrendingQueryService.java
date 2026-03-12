package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.TrendingData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Queries the most-watched media over the last N days.
 *
 * <p>
 * Uses the {@code media_popularity} read-model (watch_count + last_watched_at)
 * to determine trending content.
 * </p>
 */
@Service
public class TrendingQueryService {

    private static final int DEFAULT_DAYS = 7;
    private static final int DEFAULT_LIMIT = 10;
    private final JdbcTemplate jdbc;

    public TrendingQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the trending media for the last {@code days} days.
     *
     * @param days  how many days back to consider
     * @param limit maximum number of items
     * @return populated {@link TrendingData}
     */
    public TrendingData build(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<MediaItem> items = jdbc.query(
                "SELECT m.id, m.title, m.type, m.poster_url, "
                        + "mp.avg_rating, mp.watch_count, m.release_year "
                        + "FROM media_popularity mp "
                        + "JOIN media m ON m.id = mp.media_id "
                        + "WHERE mp.last_watched_at >= ? "
                        + "ORDER BY mp.watch_count DESC, mp.last_watched_at DESC "
                        + "LIMIT ?",
                (rs, rn) -> new MediaItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("poster_url"),
                        rs.getBigDecimal("avg_rating"),
                        rs.getObject("watch_count", Long.class),
                        rs.getObject("release_year", Integer.class)),
                since, limit);

        // fall back to overall trending if no recent data
        if (items.isEmpty()) {
            items = jdbc.query(
                    "SELECT m.id, m.title, m.type, m.poster_url, "
                            + "mp.avg_rating, mp.watch_count, m.release_year "
                            + "FROM media_popularity mp "
                            + "JOIN media m ON m.id = mp.media_id "
                            + "ORDER BY mp.watch_count DESC "
                            + "LIMIT ?",
                    (rs, rn) -> new MediaItem(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("type"),
                            rs.getString("poster_url"),
                            rs.getBigDecimal("avg_rating"),
                            rs.getObject("watch_count", Long.class),
                            rs.getObject("release_year", Integer.class)),
                    limit);
        }

        return new TrendingData(items, LocalDateTime.now(), days);
    }

    public TrendingData build() {
        return build(DEFAULT_DAYS, DEFAULT_LIMIT);
    }
}
