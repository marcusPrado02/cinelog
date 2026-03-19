package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.NewReleasesData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the "New Releases" report — media recently added to the catalog
 * (by created_at), with poster and backdrop images.
 */
@Service
public class NewReleasesQueryService {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_LIMIT = 10;
    private final JdbcTemplate jdbc;

    public NewReleasesQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public NewReleasesData build(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        Long totalNew = jdbc.queryForObject(
                "SELECT COUNT(*) FROM media WHERE created_at >= ?",
                Long.class, since);

        List<MediaItem> items = jdbc.query(
                "SELECT m.id, m.title, m.type, m.poster_url, m.backdrop_url, "
                        + "m.release_year, "
                        + "COALESCE(mp.avg_rating, 0) AS avg_rating, "
                        + "COALESCE(mp.watch_count, 0) AS watch_count "
                        + "FROM media m "
                        + "LEFT JOIN media_popularity mp ON mp.media_id = m.id "
                        + "WHERE m.created_at >= ? "
                        + "ORDER BY m.created_at DESC "
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
                since, limit);

        return new NewReleasesData(items, LocalDateTime.now(), days, limit,
                totalNew != null ? totalNew : 0);
    }

    public NewReleasesData build() {
        return build(DEFAULT_DAYS, DEFAULT_LIMIT);
    }
}
