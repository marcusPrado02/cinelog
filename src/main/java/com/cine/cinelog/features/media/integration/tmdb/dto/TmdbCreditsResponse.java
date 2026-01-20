package com.cine.cinelog.features.media.integration.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para resposta de GET /movie/{id}/credits e /tv/{id}/credits.
 */
public class TmdbCreditsResponse {

    private Integer id;

    private List<Cast> cast;

    private List<Crew> crew;

    public static class Cast {
        private Integer id;
        private String name;
        private String character;

        @JsonProperty("order")
        private Integer castOrder;

        @JsonProperty("profile_path")
        private String profilePath;

        // getters/setters…

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCharacter() {
            return character;
        }

        public void setCharacter(String character) {
            this.character = character;
        }

        public Integer getCastOrder() {
            return castOrder;
        }

        public void setCastOrder(Integer castOrder) {
            this.castOrder = castOrder;
        }

        public String getProfilePath() {
            return profilePath;
        }

        public void setProfilePath(String profilePath) {
            this.profilePath = profilePath;
        }
    }

    public static class Crew {
        private Integer id;
        private String name;
        private String job;
        private String department;

        @JsonProperty("profile_path")
        private String profilePath;

        // getters/setters…

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getJob() {
            return job;
        }

        public void setJob(String job) {
            this.job = job;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getProfilePath() {
            return profilePath;
        }

        public void setProfilePath(String profilePath) {
            this.profilePath = profilePath;
        }
    }

    // getters/setters…

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Cast> getCast() {
        return cast;
    }

    public void setCast(List<Cast> cast) {
        this.cast = cast;
    }

    public List<Crew> getCrew() {
        return crew;
    }

    public void setCrew(List<Crew> crew) {
        this.crew = crew;
    }
}
