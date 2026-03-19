package com.cine.cinelog.features.batch.scheduler;

import com.cine.cinelog.features.batch.config.BatchJobProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Agendador dos batch jobs de importação do TMDB.
 *
 * <p>
 * Cada método é controlado pelo cron e flag {@code enabled} definidos em
 * {@code cinelog.batch.jobs.*} no {@code application.yml}.
 * Os crons são lidos diretamente das propriedades via SpEL no atributo
 * {@code cron} da anotação {@code @Scheduled}.
 * </p>
 */
@Configuration
public class BatchSchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchSchedulerConfig.class);

    private final JobLauncher jobLauncher;
    private final BatchJobProperties batchProperties;
    private final Job syncGenresJob;
    private final Job importMoviesJob;
    private final Job importTvShowsJob;
    private final Job importCreditsJob;
    private final Job importSeasonsJob;
    private final Job syncReviewsJob;
    private final Job enrichMediaImagesJob;
    private final Job enrichPersonProfilesJob;

    public BatchSchedulerConfig(
            JobLauncher jobLauncher,
            BatchJobProperties batchProperties,
            @Qualifier("syncGenresJob") Job syncGenresJob,
            @Qualifier("importMoviesJob") Job importMoviesJob,
            @Qualifier("importTvShowsJob") Job importTvShowsJob,
            @Qualifier("importCreditsJob") Job importCreditsJob,
            @Qualifier("importSeasonsJob") Job importSeasonsJob,
            @Qualifier("syncReviewsJob") Job syncReviewsJob,
            @Qualifier("enrichMediaImagesJob") Job enrichMediaImagesJob,
            @Qualifier("enrichPersonProfilesJob") Job enrichPersonProfilesJob) {
        this.jobLauncher = jobLauncher;
        this.batchProperties = batchProperties;
        this.syncGenresJob = syncGenresJob;
        this.importMoviesJob = importMoviesJob;
        this.importTvShowsJob = importTvShowsJob;
        this.importCreditsJob = importCreditsJob;
        this.importSeasonsJob = importSeasonsJob;
        this.syncReviewsJob = syncReviewsJob;
        this.enrichMediaImagesJob = enrichMediaImagesJob;
        this.enrichPersonProfilesJob = enrichPersonProfilesJob;
    }

    @Scheduled(cron = "${cinelog.batch.jobs.sync-genres.cron:0 0 3 * * SUN}")
    public void scheduleSyncGenres() {
        if (!batchProperties.getJobs().getSyncGenres().isEnabled()) {
            log.debug("syncGenresJob is disabled, skipping.");
            return;
        }
        runJob(syncGenresJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.import-movies.cron:0 30 3 * * SUN}")
    public void scheduleImportMovies() {
        if (!batchProperties.getJobs().getImportMovies().isEnabled()) {
            log.debug("importMoviesJob is disabled, skipping.");
            return;
        }
        int maxPages = effectiveMaxPages(batchProperties.getJobs().getImportMovies());
        runJob(importMoviesJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("mediaType", "MOVIE")
                .addLong("maxPages", (long) maxPages)
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.import-tv-shows.cron:0 0 4 * * SUN}")
    public void scheduleImportTvShows() {
        if (!batchProperties.getJobs().getImportTvShows().isEnabled()) {
            log.debug("importTvShowsJob is disabled, skipping.");
            return;
        }
        int maxPages = effectiveMaxPages(batchProperties.getJobs().getImportTvShows());
        runJob(importTvShowsJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("mediaType", "SERIES")
                .addLong("maxPages", (long) maxPages)
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.import-credits.cron:0 30 4 * * SUN}")
    public void scheduleImportCredits() {
        if (!batchProperties.getJobs().getImportCredits().isEnabled()) {
            log.debug("importCreditsJob is disabled, skipping.");
            return;
        }
        runJob(importCreditsJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.import-seasons.cron:0 0 5 * * SUN}")
    public void scheduleImportSeasons() {
        if (!batchProperties.getJobs().getImportSeasons().isEnabled()) {
            log.debug("importSeasonsJob is disabled, skipping.");
            return;
        }
        runJob(importSeasonsJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.sync-reviews.cron:0 30 5 * * SUN}")
    public void scheduleSyncReviews() {
        if (!batchProperties.getJobs().getSyncReviews().isEnabled()) {
            log.debug("syncReviewsJob is disabled, skipping.");
            return;
        }
        runJob(syncReviewsJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.enrich-media-images.cron:0 0 6 * * SUN}")
    public void scheduleEnrichMediaImages() {
        if (!batchProperties.getJobs().getEnrichMediaImages().isEnabled()) {
            log.debug("enrichMediaImagesJob is disabled, skipping.");
            return;
        }
        runJob(enrichMediaImagesJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }

    @Scheduled(cron = "${cinelog.batch.jobs.enrich-person-profiles.cron:0 30 6 * * SUN}")
    public void scheduleEnrichPersonProfiles() {
        if (!batchProperties.getJobs().getEnrichPersonProfiles().isEnabled()) {
            log.debug("enrichPersonProfilesJob is disabled, skipping.");
            return;
        }
        runJob(enrichPersonProfilesJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }

    // -------------------------------------------------------------------------

    private void runJob(Job job, JobParameters params) {
        try {
            var execution = jobLauncher.run(job, params);
            log.info("Scheduled job started: {} executionId={} status={}",
                    job.getName(), execution.getId(), execution.getStatus());
        } catch (Exception e) {
            log.error("Failed to launch scheduled job {}: {}", job.getName(), e.getMessage(), e);
        }
    }

    private int effectiveMaxPages(BatchJobProperties.JobConfig jobConfig) {
        int custom = jobConfig.getMaxPages();
        return custom > 0 ? custom : batchProperties.getMaxPages();
    }
}
