package com.cine.cinelog.features.batch.web;

import com.cine.cinelog.features.batch.config.BatchJobProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para disparo manual dos batch jobs de sincronização com o
 * TMDB.
 *
 * <p>
 * Todos os endpoints requerem papel ADMIN e lançam jobs de forma assíncrona.
 * Retorna o executionId para rastreamento.
 * </p>
 *
 * @deprecated Usar SCDF Dashboard (http://localhost:9393/dashboard) ou SCDF REST API
 * para disparo e agendamento de jobs. Estes endpoints serao removidos em versao futura.
 */
@Deprecated(since = "1.0", forRemoval = true)
@Tag(name = "Admin Batch", description = "Gerenciamento dos batch jobs de importação TMDB (deprecated — use SCDF)")
@RestController
@RequestMapping("/api/v1/admin/batch")
@PreAuthorize("hasRole('ADMIN')")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

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
    private final Job linkTmdbJob;

    public BatchJobController(
            JobLauncher jobLauncher,
            BatchJobProperties batchProperties,
            @Qualifier("syncGenresJob") Job syncGenresJob,
            @Qualifier("importMoviesJob") Job importMoviesJob,
            @Qualifier("importTvShowsJob") Job importTvShowsJob,
            @Qualifier("importCreditsJob") Job importCreditsJob,
            @Qualifier("importSeasonsJob") Job importSeasonsJob,
            @Qualifier("syncReviewsJob") Job syncReviewsJob,
            @Qualifier("enrichMediaImagesJob") Job enrichMediaImagesJob,
            @Qualifier("enrichPersonProfilesJob") Job enrichPersonProfilesJob,
            @Qualifier("linkTmdbJob") Job linkTmdbJob) {
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
        this.linkTmdbJob = linkTmdbJob;
    }

    @Operation(summary = "Sincronizar gêneros do TMDB")
    @PostMapping("/genres")
    public ResponseEntity<Map<String, Object>> syncGenres() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(syncGenresJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Importar filmes do TMDB")
    @PostMapping("/movies")
    public ResponseEntity<Map<String, Object>> importMovies(
            @RequestParam(name = "maxPages", required = false) Integer maxPages) throws Exception {
        int pages = maxPages != null ? maxPages
                : effectiveMaxPages(batchProperties.getJobs().getImportMovies());
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("mediaType", "MOVIE")
                .addLong("maxPages", (long) pages)
                .toJobParameters();
        JobExecution execution = jobLauncher.run(importMoviesJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Importar séries de TV do TMDB")
    @PostMapping("/tv-shows")
    public ResponseEntity<Map<String, Object>> importTvShows(
            @RequestParam(name = "maxPages", required = false) Integer maxPages) throws Exception {
        int pages = maxPages != null ? maxPages
                : effectiveMaxPages(batchProperties.getJobs().getImportTvShows());
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("mediaType", "SERIES")
                .addLong("maxPages", (long) pages)
                .toJobParameters();
        JobExecution execution = jobLauncher.run(importTvShowsJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Importar créditos (elenco/equipe) do TMDB")
    @PostMapping("/credits")
    public ResponseEntity<Map<String, Object>> importCredits() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(importCreditsJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Importar temporadas e episódios do TMDB")
    @PostMapping("/seasons")
    public ResponseEntity<Map<String, Object>> importSeasons() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(importSeasonsJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Sincronizar reviews do TMDB para watch_entry")
    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> syncReviews() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(syncReviewsJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Enriquecer imagens de mídias via TMDB")
    @PostMapping("/enrich-images")
    public ResponseEntity<Map<String, Object>> enrichMediaImages() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(enrichMediaImagesJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Enriquecer perfis de pessoas via TMDB")
    @PostMapping("/enrich-profiles")
    public ResponseEntity<Map<String, Object>> enrichPersonProfiles() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(enrichPersonProfilesJob, params);
        return buildResponse(execution);
    }

    @Operation(summary = "Vincular midias de seed ao TMDB pelo titulo e preencher imagens")
    @PostMapping("/link-tmdb")
    public ResponseEntity<Map<String, Object>> linkTmdb() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(linkTmdbJob, params);
        return buildResponse(execution);
    }

    // -------------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> buildResponse(JobExecution execution) {
        log.info("Job launched: {} executionId={} status={}",
                execution.getJobInstance().getJobName(),
                execution.getId(),
                execution.getStatus());
        return ResponseEntity.ok(Map.of(
                "executionId", execution.getId(),
                "jobName", execution.getJobInstance().getJobName(),
                "status", execution.getStatus().toString()));
    }

    private int effectiveMaxPages(BatchJobProperties.JobConfig jobConfig) {
        int custom = jobConfig.getMaxPages();
        return custom > 0 ? custom : batchProperties.getMaxPages();
    }
}
