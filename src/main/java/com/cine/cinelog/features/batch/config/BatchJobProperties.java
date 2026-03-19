package com.cine.cinelog.features.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração para os batch jobs de importação do TMDB.
 *
 * <p>
 * Prefixo: {@code cinelog.batch}
 * </p>
 * <p>
 * Configurado em {@code application.yml}.
 * </p>
 */
@ConfigurationProperties(prefix = "cinelog.batch")
public class BatchJobProperties {

    /** Máximo de páginas por execução (padrão global). */
    private int maxPages = 10;

    /** Critério de ordenação para discover (ex.: popularity.desc). */
    private String sortBy = "popularity.desc";

    /** Tamanho de chunk para processamento em lote. */
    private int chunkSize = 20;

    /** Configurações por job. */
    private Jobs jobs = new Jobs();

    // =============== getters / setters ===============

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Jobs getJobs() {
        return jobs;
    }

    public void setJobs(Jobs jobs) {
        this.jobs = jobs;
    }

    // ================================================================
    // Nested classes
    // ================================================================

    public static class Jobs {

        private JobConfig syncGenres = new JobConfig("0 0 3 * * SUN", true, 1);
        private JobConfig importMovies = new JobConfig("0 30 3 * * SUN", true, 10);
        private JobConfig importTvShows = new JobConfig("0 0 4 * * SUN", true, 10);
        private JobConfig importCredits = new JobConfig("0 30 4 * * SUN", true, 0);
        private JobConfig importSeasons = new JobConfig("0 0 5 * * SUN", true, 0);
        private JobConfig syncReviews = new JobConfig("0 30 5 * * SUN", true, 0);
        private JobConfig enrichMediaImages = new JobConfig("0 0 6 * * SUN", true, 0);
        private JobConfig enrichPersonProfiles = new JobConfig("0 30 6 * * SUN", true, 0);

        public JobConfig getSyncGenres() {
            return syncGenres;
        }

        public void setSyncGenres(JobConfig syncGenres) {
            this.syncGenres = syncGenres;
        }

        public JobConfig getImportMovies() {
            return importMovies;
        }

        public void setImportMovies(JobConfig importMovies) {
            this.importMovies = importMovies;
        }

        public JobConfig getImportTvShows() {
            return importTvShows;
        }

        public void setImportTvShows(JobConfig importTvShows) {
            this.importTvShows = importTvShows;
        }

        public JobConfig getImportCredits() {
            return importCredits;
        }

        public void setImportCredits(JobConfig importCredits) {
            this.importCredits = importCredits;
        }

        public JobConfig getImportSeasons() {
            return importSeasons;
        }

        public void setImportSeasons(JobConfig importSeasons) {
            this.importSeasons = importSeasons;
        }

        public JobConfig getSyncReviews() {
            return syncReviews;
        }

        public void setSyncReviews(JobConfig syncReviews) {
            this.syncReviews = syncReviews;
        }

        public JobConfig getEnrichMediaImages() {
            return enrichMediaImages;
        }

        public void setEnrichMediaImages(JobConfig enrichMediaImages) {
            this.enrichMediaImages = enrichMediaImages;
        }

        public JobConfig getEnrichPersonProfiles() {
            return enrichPersonProfiles;
        }

        public void setEnrichPersonProfiles(JobConfig enrichPersonProfiles) {
            this.enrichPersonProfiles = enrichPersonProfiles;
        }
    }

    public static class JobConfig {

        private String cron;
        private boolean enabled;
        /** Máximo de páginas para este job (0 = use valor global). */
        private int maxPages;

        public JobConfig() {
        }

        public JobConfig(String cron, boolean enabled, int maxPages) {
            this.cron = cron;
            this.enabled = enabled;
            this.maxPages = maxPages;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }
    }
}
