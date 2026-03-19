package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.PlatformReportData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the admin platform report combining global statistics.
 */
@Service
public class PlatformReportQueryService {

    private final JdbcTemplate jdbc;
    private final TopRatedQueryService topRatedQueryService;
    private final TrendingQueryService trendingQueryService;

    public PlatformReportQueryService(JdbcTemplate jdbc,
            TopRatedQueryService topRatedQueryService,
            TrendingQueryService trendingQueryService) {
        this.jdbc = jdbc;
        this.topRatedQueryService = topRatedQueryService;
        this.trendingQueryService = trendingQueryService;
    }

    /**
     * Builds a complete platform report.
     *
     * @return populated {@link PlatformReportData}
     */
    public PlatformReportData build() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime weekAgoDate = weekAgo;

        Long totalUsers = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        Long totalMedia = jdbc.queryForObject("SELECT COUNT(*) FROM media", Long.class);
        Long totalWatchEntries = jdbc.queryForObject("SELECT COUNT(*) FROM watch_entry", Long.class);

        Long newUsersThisWeek = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE created_at >= ?", Long.class, weekAgoDate);
        Long newMediaThisWeek = jdbc.queryForObject(
                "SELECT COUNT(*) FROM media WHERE created_at >= ?", Long.class, weekAgoDate);
        Long newWatchEntriesThisWeek = jdbc.queryForObject(
                "SELECT COUNT(*) FROM watch_entry WHERE watched_at >= ?", Long.class, weekAgo.toLocalDate());

        List<MediaItem> topRated = topRatedQueryService.build(5).getItems();
        List<MediaItem> trending = trendingQueryService.build(7, 5).getItems();

        PlatformReportData data = new PlatformReportData();
        data.setTotalUsers(totalUsers != null ? totalUsers : 0L);
        data.setTotalMedia(totalMedia != null ? totalMedia : 0L);
        data.setTotalWatchEntries(totalWatchEntries != null ? totalWatchEntries : 0L);
        data.setNewUsersThisWeek(newUsersThisWeek != null ? newUsersThisWeek : 0L);
        data.setNewMediaThisWeek(newMediaThisWeek != null ? newMediaThisWeek : 0L);
        data.setNewWatchEntriesThisWeek(newWatchEntriesThisWeek != null ? newWatchEntriesThisWeek : 0L);
        data.setTopRatedMedia(topRated);
        data.setTrendingMedia(trending);
        data.setGeneratedAt(LocalDateTime.now());
        return data;
    }
}
