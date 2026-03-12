package com.cine.cinelog.features.reports.data;

import java.util.List;

/**
 * Data for the personalized recommendations email report.
 */
public class RecommendationsData {

    private Long userId;
    private String userName;
    private String userEmail;

    private List<String> favoriteGenres;
    private List<MediaItem> recommendations;

    public RecommendationsData() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public List<MediaItem> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<MediaItem> recommendations) {
        this.recommendations = recommendations;
    }
}
