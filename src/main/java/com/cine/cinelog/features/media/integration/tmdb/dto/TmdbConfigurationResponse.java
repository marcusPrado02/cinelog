package com.cine.cinelog.features.media.integration.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para resposta de GET /configuration.
 */
public class TmdbConfigurationResponse {

    private Images images;

    public static class Images {

        @JsonProperty("base_url")
        private String baseUrl;

        @JsonProperty("secure_base_url")
        private String secureBaseUrl;

        @JsonProperty("poster_sizes")
        private List<String> posterSizes;

        @JsonProperty("backdrop_sizes")
        private List<String> backdropSizes;

        @JsonProperty("profile_sizes")
        private List<String> profileSizes;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSecureBaseUrl() {
            return secureBaseUrl;
        }

        public void setSecureBaseUrl(String secureBaseUrl) {
            this.secureBaseUrl = secureBaseUrl;
        }

        public List<String> getPosterSizes() {
            return posterSizes;
        }

        public void setPosterSizes(List<String> posterSizes) {
            this.posterSizes = posterSizes;
        }

        public List<String> getBackdropSizes() {
            return backdropSizes;
        }

        public void setBackdropSizes(List<String> backdropSizes) {
            this.backdropSizes = backdropSizes;
        }

        public List<String> getProfileSizes() {
            return profileSizes;
        }

        public void setProfileSizes(List<String> profileSizes) {
            this.profileSizes = profileSizes;
        }
    }

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }
}
