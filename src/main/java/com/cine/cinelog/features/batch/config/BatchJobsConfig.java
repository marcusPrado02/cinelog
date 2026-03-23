package com.cine.cinelog.features.batch.config;

import com.cine.cinelog.features.batch.jobs.credits.CreditsItemWriter;
import com.cine.cinelog.features.batch.jobs.credits.MediaWithTmdbIdItemReader;
import com.cine.cinelog.features.batch.jobs.credits.TmdbCreditsBundle;
import com.cine.cinelog.features.batch.jobs.credits.TmdbCreditsItemProcessor;
import com.cine.cinelog.features.batch.jobs.genres.SyncGenresTasklet;
import com.cine.cinelog.features.batch.jobs.media.LinkTmdbTasklet;
import com.cine.cinelog.features.batch.jobs.images.MediaImageUpdateWriter;
import com.cine.cinelog.features.batch.jobs.images.MediaMissingImagesItemReader;
import com.cine.cinelog.features.batch.jobs.images.TmdbMediaImageProcessor;
import com.cine.cinelog.features.batch.jobs.media.MediaItemWriter;
import com.cine.cinelog.features.batch.jobs.media.MediaWithGenres;
import com.cine.cinelog.features.batch.jobs.media.TmdbMediaItemProcessor;
import com.cine.cinelog.features.batch.jobs.media.TmdbMediaPageReader;
import com.cine.cinelog.features.batch.jobs.people.PersonMissingProfileItemReader;
import com.cine.cinelog.features.batch.jobs.people.PersonProfileUpdateWriter;
import com.cine.cinelog.features.batch.jobs.people.TmdbPersonDetailsProcessor;
import com.cine.cinelog.features.batch.jobs.reviews.ReviewsBundle;
import com.cine.cinelog.features.batch.jobs.reviews.ReviewsMediaItemReader;
import com.cine.cinelog.features.batch.jobs.reviews.TmdbReviewsItemProcessor;
import com.cine.cinelog.features.batch.jobs.reviews.TmdbReviewsItemWriter;
import com.cine.cinelog.features.batch.jobs.seasons.SeasonsEpisodesItemWriter;
import com.cine.cinelog.features.batch.jobs.seasons.TmdbSeasonsBundle;
import com.cine.cinelog.features.batch.jobs.seasons.TmdbSeasonsItemProcessor;
import com.cine.cinelog.features.batch.jobs.seasons.TvSeriesItemReader;
import com.cine.cinelog.features.batch.metrics.BatchJobMetricsListener;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Configuracao central dos Spring Batch Jobs para importacao de dados do TMDB.
 *
 * <p>Cada job utiliza o padrao Spring Batch 5 com JobBuilder/StepBuilder
 * injetando JobRepository e PlatformTransactionManager diretamente.</p>
 *
 * <p>Steps chunk-oriented incluem:
 * <ul>
 *   <li><b>Retry</b> com backoff exponencial para erros transientes de rede
 *       (WebClientResponseException, RestClientException, timeouts)</li>
 *   <li><b>Skip</b> para itens com dados corrompidos apos esgotadas as tentativas</li>
 *   <li><b>metricsListener</b> registrado em Job e Step para metricas Micrometer</li>
 * </ul>
 * </p>
 *
 * @see BatchJobProperties configuracao externalizada (retry, skip, chunk)
 * @see BatchJobMetricsListener metricas por job e step
 */
@Configuration
public class BatchJobsConfig {

    private final BatchJobProperties props;
    private final BatchJobMetricsListener metricsListener;

    public BatchJobsConfig(BatchJobProperties props, BatchJobMetricsListener metricsListener) {
        this.props = props;
        this.metricsListener = metricsListener;
    }

    // =========================================================================
    // Retry backoff policy (shared across all steps)
    // =========================================================================

    private ExponentialBackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(props.getRetryBackoffInitialMs());
        policy.setMaxInterval(props.getRetryBackoffMaxMs());
        policy.setMultiplier(props.getRetryBackoffMultiplier());
        return policy;
    }

    private <I, O> FaultTolerantStepBuilder<I, O> applyFaultTolerance(
            FaultTolerantStepBuilder<I, O> builder, int skipLimit) {
        return builder
                .retry(WebClientResponseException.class)
                .retry(RestClientException.class)
                .retry(ConnectException.class)
                .retry(SocketTimeoutException.class)
                .retryLimit(props.getRetryLimit())
                .backOffPolicy(backOffPolicy())
                .skip(Exception.class)
                .skipLimit(skipLimit);
    }

    // =========================================================================
    // Job: linkTmdbJob — vincula midias de seed ao TMDB pelo titulo
    // =========================================================================

    @Bean
    public Job linkTmdbJob(JobRepository jobRepository, Step linkTmdbStep) {
        return new JobBuilder("linkTmdbJob", jobRepository)
                .listener(metricsListener)
                .start(linkTmdbStep)
                .build();
    }

    @Bean
    public Step linkTmdbStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            LinkTmdbTasklet linkTmdbTasklet) {
        return new StepBuilder("linkTmdbStep", jobRepository)
                .tasklet(linkTmdbTasklet, txManager)
                .listener(metricsListener)
                .build();
    }

    // =========================================================================
    // Job: syncGenresJob
    // =========================================================================

    @Bean
    public Job syncGenresJob(JobRepository jobRepository,
            Step syncGenresStep) {
        return new JobBuilder("syncGenresJob", jobRepository)
                .listener(metricsListener)
                .start(syncGenresStep)
                .build();
    }

    @Bean
    public Step syncGenresStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            SyncGenresTasklet syncGenresTasklet) {
        return new StepBuilder("syncGenresStep", jobRepository)
                .tasklet(syncGenresTasklet, txManager)
                .listener(metricsListener)
                .build();
    }

    // =========================================================================
    // Job: importMoviesJob
    // =========================================================================

    @Bean
    public Job importMoviesJob(JobRepository jobRepository,
            Step importMoviesStep) {
        return new JobBuilder("importMoviesJob", jobRepository)
                .listener(metricsListener)
                .start(importMoviesStep)
                .build();
    }

    @Bean
    public Step importMoviesStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            TmdbMediaPageReader tmdbMediaPageReader,
            TmdbMediaItemProcessor tmdbMediaItemProcessor,
            MediaItemWriter mediaItemWriter) {
        return applyFaultTolerance(
                new StepBuilder("importMoviesStep", jobRepository)
                    .<TmdbMediaSummary, MediaWithGenres>chunk(props.getChunkSize(), txManager)
                    .reader(tmdbMediaPageReader)
                    .processor(tmdbMediaItemProcessor)
                    .writer(mediaItemWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: importTvShowsJob
    // =========================================================================

    @Bean
    public Job importTvShowsJob(JobRepository jobRepository,
            Step importTvShowsStep) {
        return new JobBuilder("importTvShowsJob", jobRepository)
                .listener(metricsListener)
                .start(importTvShowsStep)
                .build();
    }

    @Bean
    public Step importTvShowsStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            TmdbMediaPageReader tmdbMediaPageReader,
            TmdbMediaItemProcessor tmdbMediaItemProcessor,
            MediaItemWriter mediaItemWriter) {
        return applyFaultTolerance(
                new StepBuilder("importTvShowsStep", jobRepository)
                    .<TmdbMediaSummary, MediaWithGenres>chunk(props.getChunkSize(), txManager)
                    .reader(tmdbMediaPageReader)
                    .processor(tmdbMediaItemProcessor)
                    .writer(mediaItemWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: importCreditsJob
    // =========================================================================

    @Bean
    public Job importCreditsJob(JobRepository jobRepository,
            Step importCreditsStep) {
        return new JobBuilder("importCreditsJob", jobRepository)
                .listener(metricsListener)
                .start(importCreditsStep)
                .build();
    }

    @Bean
    public Step importCreditsStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            MediaWithTmdbIdItemReader mediaWithTmdbIdItemReader,
            TmdbCreditsItemProcessor tmdbCreditsItemProcessor,
            CreditsItemWriter creditsItemWriter) {
        return applyFaultTolerance(
                new StepBuilder("importCreditsStep", jobRepository)
                    .<Media, TmdbCreditsBundle>chunk(props.getChunkSize(), txManager)
                    .reader(mediaWithTmdbIdItemReader)
                    .processor(tmdbCreditsItemProcessor)
                    .writer(creditsItemWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: importSeasonsJob
    // =========================================================================

    @Bean
    public Job importSeasonsJob(JobRepository jobRepository,
            Step importSeasonsStep) {
        return new JobBuilder("importSeasonsJob", jobRepository)
                .listener(metricsListener)
                .start(importSeasonsStep)
                .build();
    }

    @Bean
    public Step importSeasonsStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            TvSeriesItemReader tvSeriesItemReader,
            TmdbSeasonsItemProcessor tmdbSeasonsItemProcessor,
            SeasonsEpisodesItemWriter seasonsEpisodesItemWriter) {
        return applyFaultTolerance(
                new StepBuilder("importSeasonsStep", jobRepository)
                    .<Media, TmdbSeasonsBundle>chunk(props.getChunkSize(), txManager)
                    .reader(tvSeriesItemReader)
                    .processor(tmdbSeasonsItemProcessor)
                    .writer(seasonsEpisodesItemWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: syncReviewsJob
    // =========================================================================

    @Bean
    public Job syncReviewsJob(JobRepository jobRepository, Step syncReviewsStep) {
        return new JobBuilder("syncReviewsJob", jobRepository)
                .listener(metricsListener)
                .start(syncReviewsStep)
                .build();
    }

    @Bean
    public Step syncReviewsStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            ReviewsMediaItemReader reviewsMediaItemReader,
            TmdbReviewsItemProcessor tmdbReviewsItemProcessor,
            TmdbReviewsItemWriter tmdbReviewsItemWriter) {
        return applyFaultTolerance(
                new StepBuilder("syncReviewsStep", jobRepository)
                    .<Media, ReviewsBundle>chunk(props.getChunkSize(), txManager)
                    .reader(reviewsMediaItemReader)
                    .processor(tmdbReviewsItemProcessor)
                    .writer(tmdbReviewsItemWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getReviewsSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: enrichMediaImagesJob
    // =========================================================================

    @Bean
    public Job enrichMediaImagesJob(JobRepository jobRepository, Step enrichMediaImagesStep) {
        return new JobBuilder("enrichMediaImagesJob", jobRepository)
                .listener(metricsListener)
                .start(enrichMediaImagesStep)
                .build();
    }

    @Bean
    public Step enrichMediaImagesStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            MediaMissingImagesItemReader mediaMissingImagesItemReader,
            TmdbMediaImageProcessor tmdbMediaImageProcessor,
            MediaImageUpdateWriter mediaImageUpdateWriter) {
        return applyFaultTolerance(
                new StepBuilder("enrichMediaImagesStep", jobRepository)
                    .<Media, Media>chunk(props.getChunkSize(), txManager)
                    .reader(mediaMissingImagesItemReader)
                    .processor(tmdbMediaImageProcessor)
                    .writer(mediaImageUpdateWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: enrichPersonProfilesJob
    // =========================================================================

    @Bean
    public Job enrichPersonProfilesJob(JobRepository jobRepository, Step enrichPersonProfilesStep) {
        return new JobBuilder("enrichPersonProfilesJob", jobRepository)
                .listener(metricsListener)
                .start(enrichPersonProfilesStep)
                .build();
    }

    @Bean
    public Step enrichPersonProfilesStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            PersonMissingProfileItemReader personMissingProfileItemReader,
            TmdbPersonDetailsProcessor tmdbPersonDetailsProcessor,
            PersonProfileUpdateWriter personProfileUpdateWriter) {
        return applyFaultTolerance(
                new StepBuilder("enrichPersonProfilesStep", jobRepository)
                    .<Person, Person>chunk(props.getChunkSize(), txManager)
                    .reader(personMissingProfileItemReader)
                    .processor(tmdbPersonDetailsProcessor)
                    .writer(personProfileUpdateWriter)
                    .listener(metricsListener)
                    .faultTolerant(),
                props.getSkipLimit())
                .build();
    }

    // =========================================================================
    // Job: sendWeeklyDigestJob — envia digest semanal por e-mail
    // =========================================================================

    @Bean
    public Job sendWeeklyDigestJob(JobRepository jobRepository, Step sendWeeklyDigestStep) {
        return new JobBuilder("sendWeeklyDigestJob", jobRepository)
                .listener(metricsListener)
                .start(sendWeeklyDigestStep)
                .build();
    }

    @Bean
    public Step sendWeeklyDigestStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            com.cine.cinelog.features.batch.jobs.reports.WeeklyDigestTasklet tasklet) {
        return new StepBuilder("sendWeeklyDigestStep", jobRepository)
                .tasklet(tasklet, txManager)
                .allowStartIfComplete(true)
                .listener(metricsListener)
                .build();
    }

    // =========================================================================
    // Job: sendTrendingReportJob — envia trending report por e-mail
    // =========================================================================

    @Bean
    public Job sendTrendingReportJob(JobRepository jobRepository, Step sendTrendingReportStep) {
        return new JobBuilder("sendTrendingReportJob", jobRepository)
                .listener(metricsListener)
                .start(sendTrendingReportStep)
                .build();
    }

    @Bean
    public Step sendTrendingReportStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            com.cine.cinelog.features.batch.jobs.reports.TrendingReportTasklet tasklet) {
        return new StepBuilder("sendTrendingReportStep", jobRepository)
                .tasklet(tasklet, txManager)
                .allowStartIfComplete(true)
                .listener(metricsListener)
                .build();
    }

    // =========================================================================
    // Job: sendPlatformReportJob — envia relatorio da plataforma para admin
    // =========================================================================

    @Bean
    public Job sendPlatformReportJob(JobRepository jobRepository, Step sendPlatformReportStep) {
        return new JobBuilder("sendPlatformReportJob", jobRepository)
                .listener(metricsListener)
                .start(sendPlatformReportStep)
                .build();
    }

    @Bean
    public Step sendPlatformReportStep(JobRepository jobRepository,
            PlatformTransactionManager txManager,
            com.cine.cinelog.features.batch.jobs.reports.PlatformReportTasklet tasklet) {
        return new StepBuilder("sendPlatformReportStep", jobRepository)
                .tasklet(tasklet, txManager)
                .allowStartIfComplete(true)
                .listener(metricsListener)
                .build();
    }
}
