package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.WeeklyDigestData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the weekly digest data for a given user.
 *
 * <p>
 * Queries watch_entries for activity in the current week plus user_stats
 * for overall averages and favorite genre.
 * </p>
 */
@Service
public class WeeklyDigestQueryService {

        private final JdbcTemplate jdbc;

        public WeeklyDigestQueryService(JdbcTemplate jdbc) {
                this.jdbc = jdbc;
        }

        /**
         * Builds a {@link WeeklyDigestData} for the specified user.
         *
         * @param userId   the user's id
         * @param userName the user's display name
         * @param email    the user's email address
         * @return populated digest data
         */
        public WeeklyDigestData build(Long userId, String userName, String email) {
                LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
                LocalDate weekEnd = weekStart.plusDays(6);

                // counts for the current week
                Long totalThisWeek = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM watch_entry WHERE user_id = ? AND watched_at >= ? AND watched_at <= ?",
                                Long.class, userId, weekStart, weekEnd);

                Long moviesThisWeek = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM watch_entry we "
                                                + "JOIN media m ON m.id = we.media_id "
                                                + "WHERE we.user_id = ? AND we.watched_at >= ? AND we.watched_at <= ? "
                                                + "AND m.type = 'MOVIE'",
                                Long.class, userId, weekStart, weekEnd);

                Long seriesThisWeek = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM watch_entry we "
                                                + "JOIN media m ON m.id = we.media_id "
                                                + "WHERE we.user_id = ? AND we.watched_at >= ? AND we.watched_at <= ? "
                                                + "AND m.type = 'TV_SHOW'",
                                Long.class, userId, weekStart, weekEnd);

                // overall avg rating from read-model (null-safe: user may have no stats yet)
                BigDecimal avgRating = jdbc.query(
                                "SELECT avg_rating FROM user_stats WHERE user_id = ?",
                                rs -> rs.next() ? rs.getBigDecimal(1) : null,
                                userId);

                // recently watched this week (last 5 entries)
                List<MediaItem> recentlyWatched = jdbc.query(
                                "SELECT m.id, m.title, m.type, m.poster_url, m.backdrop_url, mp.avg_rating, mp.watch_count, m.release_year "
                                                + "FROM watch_entry we "
                                                + "JOIN media m ON m.id = we.media_id "
                                                + "LEFT JOIN media_popularity mp ON mp.media_id = m.id "
                                                + "WHERE we.user_id = ? AND we.watched_at >= ? AND we.watched_at <= ? "
                                                + "ORDER BY we.watched_at DESC, we.id DESC "
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
                                userId, weekStart, weekEnd);

                // favorite genre this week
                String favoriteGenre = null;
                List<String> genres = jdbc.queryForList(
                                "SELECT g.name FROM watch_entry we "
                                                + "JOIN media m ON m.id = we.media_id "
                                                + "JOIN media_genres mg ON mg.media_id = m.id "
                                                + "JOIN genres g ON g.id = mg.genre_id "
                                                + "WHERE we.user_id = ? AND we.watched_at >= ? AND we.watched_at <= ? "
                                                + "GROUP BY g.name ORDER BY COUNT(*) DESC LIMIT 1",
                                String.class, userId, weekStart, weekEnd);
                if (!genres.isEmpty()) {
                        favoriteGenre = genres.get(0);
                }

                WeeklyDigestData data = new WeeklyDigestData();
                data.setUserId(userId);
                data.setUserName(userName);
                data.setUserEmail(email);
                data.setTotalWatchedThisWeek(totalThisWeek != null ? totalThisWeek : 0L);
                data.setTotalMoviesThisWeek(moviesThisWeek != null ? moviesThisWeek : 0L);
                data.setTotalSeriesThisWeek(seriesThisWeek != null ? seriesThisWeek : 0L);
                data.setAvgRatingOverall(avgRating);
                data.setWeekStart(weekStart);
                data.setWeekEnd(weekEnd);
                data.setRecentlyWatched(recentlyWatched);
                data.setFavoriteGenre(favoriteGenre);
                return data;
        }

        /**
         * Builds digest data for a user identified only by id, loading name/email from
         * DB.
         */
        public WeeklyDigestData buildForUser(Long userId) {
                return jdbc.queryForObject(
                                "SELECT id, name, email FROM users WHERE id = ?",
                                (rs, rn) -> build(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                                userId);
        }
}
