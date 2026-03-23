package com.cine.cinelog.features.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Listener de métricas para jobs e steps do Spring Batch.
 *
 * <p>Registra no Micrometer (Prometheus) as seguintes métricas por job/step:</p>
 * <ul>
 *   <li>{@code batch_job_duration_seconds} — tempo total de execução do job</li>
 *   <li>{@code batch_job_completed_total} — contador de jobs concluídos (por status)</li>
 *   <li>{@code batch_step_items_read_total} — total de itens lidos por step</li>
 *   <li>{@code batch_step_items_written_total} — total de itens gravados por step</li>
 *   <li>{@code batch_step_items_skipped_total} — total de itens pulados por step</li>
 *   <li>{@code batch_step_duration_seconds} — duração do step</li>
 * </ul>
 *
 * <p>Todas as métricas carregam a tag {@code job_name} (e {@code step_name} nos steps),
 * permitindo filtros granulares no Grafana/Prometheus.</p>
 *
 * @see BatchJobsConfig — onde este listener é registrado nos jobs
 */
@Component
public class BatchJobMetricsListener implements JobExecutionListener, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchJobMetricsListener.class);

    private static final String TAG_JOB  = "job_name";
    private static final String TAG_STEP = "step_name";
    private static final String TAG_STATUS = "status";

    private final MeterRegistry meterRegistry;

    public BatchJobMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JobExecutionListener
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("[BATCH] Job iniciado: job={} executionId={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status  = jobExecution.getStatus().name();

        // ── duração do job ──────────────────────────────────────────────────
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            long durationMs = Duration.between(
                    jobExecution.getStartTime(),
                    jobExecution.getEndTime()).toMillis();

            Timer.builder("batch_job_duration_seconds")
                    .description("Duração total do job Spring Batch em segundos")
                    .tag(TAG_JOB, jobName)
                    .tag(TAG_STATUS, status)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }

        // ── contador de conclusão ───────────────────────────────────────────
        Counter.builder("batch_job_completed_total")
                .description("Total de execuções de jobs Spring Batch por status")
                .tag(TAG_JOB, jobName)
                .tag(TAG_STATUS, status)
                .register(meterRegistry)
                .increment();

        // ── log estruturado ─────────────────────────────────────────────────
        long totalRead    = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getReadCount).sum();
        long totalWritten = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount).sum();
        long totalSkipped = jobExecution.getStepExecutions().stream()
                .mapToLong(se -> se.getReadSkipCount() + se.getWriteSkipCount()).sum();

        log.info("[BATCH] Job concluído: job={} status={} itemsRead={} itemsWritten={} itemsSkipped={}",
                jobName, status, totalRead, totalWritten, totalSkipped);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // StepExecutionListener
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.debug("[BATCH] Step iniciado: job={} step={}",
                stepExecution.getJobExecution().getJobInstance().getJobName(),
                stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName  = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();
        String status   = stepExecution.getStatus().name();

        // ── itens lidos / gravados / pulados ────────────────────────────────
        Counter.builder("batch_step_items_read_total")
                .description("Total de itens lidos em um step Spring Batch")
                .tag(TAG_JOB, jobName).tag(TAG_STEP, stepName)
                .register(meterRegistry)
                .increment(stepExecution.getReadCount());

        Counter.builder("batch_step_items_written_total")
                .description("Total de itens gravados em um step Spring Batch")
                .tag(TAG_JOB, jobName).tag(TAG_STEP, stepName)
                .register(meterRegistry)
                .increment(stepExecution.getWriteCount());

        long skipped = stepExecution.getReadSkipCount() + stepExecution.getWriteSkipCount();
        if (skipped > 0) {
            Counter.builder("batch_step_items_skipped_total")
                    .description("Total de itens pulados (skip) em um step Spring Batch")
                    .tag(TAG_JOB, jobName).tag(TAG_STEP, stepName)
                    .register(meterRegistry)
                    .increment(skipped);
        }

        // ── duração do step ─────────────────────────────────────────────────
        if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
            long durationMs = Duration.between(
                    stepExecution.getStartTime(),
                    stepExecution.getEndTime()).toMillis();

            Timer.builder("batch_step_duration_seconds")
                    .description("Duração de cada step Spring Batch em segundos")
                    .tag(TAG_JOB, jobName).tag(TAG_STEP, stepName).tag(TAG_STATUS, status)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }

        log.info("[BATCH] Step concluído: job={} step={} status={} read={} written={} skipped={}",
                jobName, stepName, status,
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                skipped);

        return stepExecution.getExitStatus();
    }
}
