package com.cine.cinelog.features.batch.config;

import com.cine.cinelog.features.batch.jobs.credits.CreditsItemWriter;
import com.cine.cinelog.features.batch.jobs.credits.MediaWithTmdbIdItemReader;
import com.cine.cinelog.features.batch.jobs.credits.TmdbCreditsBundle;
import com.cine.cinelog.features.batch.jobs.credits.TmdbCreditsItemProcessor;
import com.cine.cinelog.features.batch.jobs.genres.SyncGenresTasklet;
import com.cine.cinelog.features.batch.jobs.media.MediaItemWriter;
import com.cine.cinelog.features.batch.jobs.media.MediaWithGenres;
import com.cine.cinelog.features.batch.jobs.media.TmdbMediaItemProcessor;
import com.cine.cinelog.features.batch.jobs.media.TmdbMediaPageReader;
import com.cine.cinelog.features.batch.jobs.seasons.SeasonsEpisodesItemWriter;
import com.cine.cinelog.features.batch.jobs.seasons.TmdbSeasonsBundle;
import com.cine.cinelog.features.batch.jobs.seasons.TmdbSeasonsItemProcessor;
import com.cine.cinelog.features.batch.jobs.seasons.TvSeriesItemReader;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuração central dos Spring Batch Jobs para importação de dados do TMDB.
 *
 * <p>Cada job utiliza o padrão Spring Batch 5 com JobBuilder/StepBuilder
 * injetando JobRepository e PlatformTransactionManager diretamente.</p>
 */
@Configuration
public class BatchJobsConfig {

    private final BatchJobProperties props;

    public BatchJobsConfig(BatchJobProperties props) {
        this.props = props;
    }

    // =========================================================================
    // Job: syncGenresJob
    // =========================================================================

    @Bean
    public Job syncGenresJob(JobRepository jobRepository,
                             Step syncGenresStep) {
        return new JobBuilder("syncGenresJob", jobRepository)
                .start(syncGenresStep)
                .build();
    }

    @Bean
    public Step syncGenresStep(JobRepository jobRepository,
                               PlatformTransactionManager txManager,
                               SyncGenresTasklet syncGenresTasklet) {
        return new StepBuilder("syncGenresStep", jobRepository)
                .tasklet(syncGenresTasklet, txManager)
                .build();
    }

    // =========================================================================
    // Job: importMoviesJob
    // =========================================================================

    @Bean
    public Job importMoviesJob(JobRepository jobRepository,
                               Step importMoviesStep) {
        return new JobBuilder("importMoviesJob", jobRepository)
                .start(importMoviesStep)
                .build();
    }

    @Bean
    public Step importMoviesStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 TmdbMediaPageReader tmdbMediaPageReader,
                                 TmdbMediaItemProcessor tmdbMediaItemProcessor,
                                 MediaItemWriter mediaItemWriter) {
        return new StepBuilder("importMoviesStep", jobRepository)
                .<TmdbMediaSummary, MediaWithGenres>chunk(props.getChunkSize(), txManager)
                .reader(tmdbMediaPageReader)
                .processor(tmdbMediaItemProcessor)
                .writer(mediaItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    // =========================================================================
    // Job: importTvShowsJob
    // =========================================================================

    @Bean
    public Job importTvShowsJob(JobRepository jobRepository,
                                Step importTvShowsStep) {
        return new JobBuilder("importTvShowsJob", jobRepository)
                .start(importTvShowsStep)
                .build();
    }

    @Bean
    public Step importTvShowsStep(JobRepository jobRepository,
                                  PlatformTransactionManager txManager,
                                  TmdbMediaPageReader tmdbMediaPageReader,
                                  TmdbMediaItemProcessor tmdbMediaItemProcessor,
                                  MediaItemWriter mediaItemWriter) {
        return new StepBuilder("importTvShowsStep", jobRepository)
                .<TmdbMediaSummary, MediaWithGenres>chunk(props.getChunkSize(), txManager)
                .reader(tmdbMediaPageReader)
                .processor(tmdbMediaItemProcessor)
                .writer(mediaItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    // =========================================================================
    // Job: importCreditsJob
    // =========================================================================

    @Bean
    public Job importCreditsJob(JobRepository jobRepository,
                                Step importCreditsStep) {
        return new JobBuilder("importCreditsJob", jobRepository)
                .start(importCreditsStep)
                .build();
    }

    @Bean
    public Step importCreditsStep(JobRepository jobRepository,
                                  PlatformTransactionManager txManager,
                                  MediaWithTmdbIdItemReader mediaWithTmdbIdItemReader,
                                  TmdbCreditsItemProcessor tmdbCreditsItemProcessor,
                                  CreditsItemWriter creditsItemWriter) {
        return new StepBuilder("importCreditsStep", jobRepository)
                .<Media, TmdbCreditsBundle>chunk(props.getChunkSize(), txManager)
                .reader(mediaWithTmdbIdItemReader)
                .processor(tmdbCreditsItemProcessor)
                .writer(creditsItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    // =========================================================================
    // Job: importSeasonsJob
    // =========================================================================

    @Bean
    public Job importSeasonsJob(JobRepository jobRepository,
                                Step importSeasonsStep) {
        return new JobBuilder("importSeasonsJob", jobRepository)
                .start(importSeasonsStep)
                .build();
    }

    @Bean
    public Step importSeasonsStep(JobRepository jobRepository,
                                  PlatformTransactionManager txManager,
                                  TvSeriesItemReader tvSeriesItemReader,
                                  TmdbSeasonsItemProcessor tmdbSeasonsItemProcessor,
                                  SeasonsEpisodesItemWriter seasonsEpisodesItemWriter) {
        return new StepBuilder("importSeasonsStep", jobRepository)
                .<Media, TmdbSeasonsBundle>chunk(props.getChunkSize(), txManager)
                .reader(tvSeriesItemReader)
                .processor(tmdbSeasonsItemProcessor)
                .writer(seasonsEpisodesItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }
}
