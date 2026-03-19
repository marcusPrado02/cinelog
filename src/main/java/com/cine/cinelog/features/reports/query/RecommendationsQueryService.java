package com.cine.cinelog.features.reports.query;

import com.cine.cinelog.features.reports.data.MediaItem;
import com.cine.cinelog.features.reports.data.RecommendationsData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds personalized media recommendations for a user based on their
 * most-watched genres.
 *
 * <p>
 * Algorithm:
 * <ol>
 * <li>Find the user's top 3 genres (most frequently watched)</li>
 * <li>Find media in those genres not yet watched by the user</li>
 * <li>Order by avg_rating DESC and return the top 10</li>
 * </ol>
 */
@Service
public class RecommendationsQueryService {

        private static final int RECOMMEND_LIMIT = 10;
        private final JdbcTemplate jdbc;

        public RecommendationsQueryService(JdbcTemplate jdbc) {
                this.jdbc = jdbc;
        }

        /**
         * Builds recommendations for the specified user.
         *
         * @param userId   the user's id
         * @param userName the user's display name
         * @param email    the user's email address
         * @return populated {@link RecommendationsData}
         */
        public RecommendationsData build(Long userId, String userName, String email) {
                // favorite genres
                List<String> favoriteGenres = jdbc.queryForList(
                                "SELECT g.name FROM watch_entry we "
                                                + "JOIN media m ON m.id = we.media_id "
                                                + "JOIN media_genres mg ON mg.media_id = m.id "
                                                + "JOIN genres g ON g.id = mg.genre_id "
                                                + "WHERE we.user_id = ? "
                                                + "GROUP BY g.id, g.name ORDER BY COUNT(*) DESC LIMIT 3",
                                String.class, userId);

                // media in those genres that the user hasn't watched
                List<MediaItem> recommendations;
                if (favoriteGenres.isEmpty()) {
                        // fall back to most popular globally
                        recommendations = jdbc.query(
                                        "SELECT m.id, m.title, m.type, m.poster_url, m.backdrop_url, "
                                                        + "mp.avg_rating, mp.watch_count, m.release_year "
                                                        + "FROM media m "
                                                        + "LEFT JOIN media_popularity mp ON mp.media_id = m.id "
                                                        + "ORDER BY COALESCE(mp.avg_rating, 0) DESC "
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
                                        RECOMMEND_LIMIT);
                } else {
                        // build IN clause placeholder
                        String placeholders = String.join(",",
                                        favoriteGenres.stream().map(g -> "?").toArray(String[]::new));
                        Object[] params = new Object[favoriteGenres.size() + 1];
                        for (int i = 0; i < favoriteGenres.size(); i++) {
                                params[i] = favoriteGenres.get(i);
                        }
                        params[favoriteGenres.size()] = userId;

                        recommendations = jdbc.query(
                                        "SELECT DISTINCT m.id, m.title, m.type, m.poster_url, m.backdrop_url, "
                                                        + "mp.avg_rating, mp.watch_count, m.release_year "
                                                        + "FROM media m "
                                                        + "JOIN media_genres mg ON mg.media_id = m.id "
                                                        + "JOIN genres g ON g.id = mg.genre_id "
                                                        + "LEFT JOIN media_popularity mp ON mp.media_id = m.id "
                                                        + "WHERE g.name IN (" + placeholders + ") "
                                                        + "AND m.id NOT IN ("
                                                        + "  SELECT DISTINCT we.media_id FROM watch_entry we "
                                                        + "  WHERE we.user_id = ? AND we.media_id IS NOT NULL"
                                                        + ") "
                                                        + "ORDER BY COALESCE(mp.avg_rating, 0) DESC "
                                                        + "LIMIT " + RECOMMEND_LIMIT,
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
                                        params);
                }

                RecommendationsData data = new RecommendationsData();
                data.setUserId(userId);
                data.setUserName(userName);
                data.setUserEmail(email);
                data.setFavoriteGenres(favoriteGenres);
                data.setRecommendations(recommendations);
                return data;
        }

        /**
         * Builds recommendations loading user info from the database.
         */
        public RecommendationsData buildForUser(Long userId) {
                return jdbc.queryForObject(
                                "SELECT id, name, email FROM users WHERE id = ?",
                                (rs, rn) -> build(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                                userId);
        }
}
