package com.cine.cinelog.features.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BatchJobMetricsListenerTest {

    private SimpleMeterRegistry meterRegistry;
    private BatchJobMetricsListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new BatchJobMetricsListener(meterRegistry);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JobExecution createJobExecution(String jobName, BatchStatus status) {
        JobInstance jobInstance = new JobInstance(1L, jobName);
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        jobExecution.setStatus(status);
        jobExecution.setStartTime(LocalDateTime.of(2026, 3, 18, 3, 0, 0));
        jobExecution.setEndTime(LocalDateTime.of(2026, 3, 18, 3, 2, 30));
        return jobExecution;
    }

    private StepExecution createStepExecution(JobExecution jobExecution, String stepName,
                                               int readCount, int writeCount,
                                               int readSkipCount, int writeSkipCount) {
        StepExecution stepExecution = new StepExecution(stepName, jobExecution);
        stepExecution.setStatus(BatchStatus.COMPLETED);
        stepExecution.setReadCount(readCount);
        stepExecution.setWriteCount(writeCount);
        stepExecution.setReadSkipCount(readSkipCount);
        stepExecution.setWriteSkipCount(writeSkipCount);
        stepExecution.setStartTime(LocalDateTime.of(2026, 3, 18, 3, 0, 0));
        stepExecution.setEndTime(LocalDateTime.of(2026, 3, 18, 3, 1, 0));
        return stepExecution;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JobExecutionListener tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("afterJob")
    class AfterJobTests {

        @Test
        @DisplayName("should record job duration timer on COMPLETED")
        void shouldRecordJobDuration() {
            JobExecution execution = createJobExecution("importMoviesJob", BatchStatus.COMPLETED);

            listener.afterJob(execution);

            Timer timer = meterRegistry.find("batch_job_duration_seconds")
                    .tag("job_name", "importMoviesJob")
                    .tag("status", "COMPLETED")
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.SECONDS)).isGreaterThan(0);
        }

        @Test
        @DisplayName("should increment completed counter per status")
        void shouldIncrementCompletedCounter() {
            JobExecution completed = createJobExecution("syncGenresJob", BatchStatus.COMPLETED);
            JobExecution failed = createJobExecution("syncGenresJob", BatchStatus.FAILED);

            listener.afterJob(completed);
            listener.afterJob(failed);

            Counter completedCounter = meterRegistry.find("batch_job_completed_total")
                    .tag("job_name", "syncGenresJob")
                    .tag("status", "COMPLETED")
                    .counter();
            Counter failedCounter = meterRegistry.find("batch_job_completed_total")
                    .tag("job_name", "syncGenresJob")
                    .tag("status", "FAILED")
                    .counter();

            assertThat(completedCounter).isNotNull();
            assertThat(completedCounter.count()).isEqualTo(1.0);
            assertThat(failedCounter).isNotNull();
            assertThat(failedCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should not record duration when startTime or endTime is null")
        void shouldNotRecordDurationWhenTimesNull() {
            JobInstance instance = new JobInstance(1L, "importMoviesJob");
            JobExecution execution = new JobExecution(instance, new JobParameters());
            execution.setStatus(BatchStatus.COMPLETED);
            // startTime and endTime are null

            listener.afterJob(execution);

            Timer timer = meterRegistry.find("batch_job_duration_seconds").timer();
            assertThat(timer).isNull();

            // counter should still be incremented
            Counter counter = meterRegistry.find("batch_job_completed_total")
                    .tag("status", "COMPLETED")
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should use distinct tags for different job names")
        void shouldDistinguishByJobName() {
            listener.afterJob(createJobExecution("importMoviesJob", BatchStatus.COMPLETED));
            listener.afterJob(createJobExecution("importTvShowsJob", BatchStatus.COMPLETED));

            assertThat(meterRegistry.find("batch_job_completed_total")
                    .tag("job_name", "importMoviesJob").counter()).isNotNull();
            assertThat(meterRegistry.find("batch_job_completed_total")
                    .tag("job_name", "importTvShowsJob").counter()).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // StepExecutionListener tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("afterStep")
    class AfterStepTests {

        @Test
        @DisplayName("should record items read, written and step duration")
        void shouldRecordStepMetrics() {
            JobExecution jobExec = createJobExecution("importMoviesJob", BatchStatus.COMPLETED);
            StepExecution stepExec = createStepExecution(jobExec, "importMoviesStep", 100, 98, 0, 0);

            ExitStatus result = listener.afterStep(stepExec);

            assertThat(result).isEqualTo(stepExec.getExitStatus());

            Counter readCounter = meterRegistry.find("batch_step_items_read_total")
                    .tag("job_name", "importMoviesJob")
                    .tag("step_name", "importMoviesStep")
                    .counter();
            assertThat(readCounter).isNotNull();
            assertThat(readCounter.count()).isEqualTo(100.0);

            Counter writtenCounter = meterRegistry.find("batch_step_items_written_total")
                    .tag("job_name", "importMoviesJob")
                    .tag("step_name", "importMoviesStep")
                    .counter();
            assertThat(writtenCounter).isNotNull();
            assertThat(writtenCounter.count()).isEqualTo(98.0);

            Timer timer = meterRegistry.find("batch_step_duration_seconds")
                    .tag("step_name", "importMoviesStep")
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should record skipped items only when skips > 0")
        void shouldRecordSkippedItems() {
            JobExecution jobExec = createJobExecution("importCreditsJob", BatchStatus.COMPLETED);
            StepExecution stepExec = createStepExecution(jobExec, "importCreditsStep", 50, 45, 3, 2);

            listener.afterStep(stepExec);

            Counter skippedCounter = meterRegistry.find("batch_step_items_skipped_total")
                    .tag("step_name", "importCreditsStep")
                    .counter();
            assertThat(skippedCounter).isNotNull();
            assertThat(skippedCounter.count()).isEqualTo(5.0); // 3 read + 2 write
        }

        @Test
        @DisplayName("should NOT register skipped counter when skips == 0")
        void shouldNotRegisterSkippedWhenZero() {
            JobExecution jobExec = createJobExecution("syncGenresJob", BatchStatus.COMPLETED);
            StepExecution stepExec = createStepExecution(jobExec, "syncGenresStep", 20, 20, 0, 0);

            listener.afterStep(stepExec);

            Counter skippedCounter = meterRegistry.find("batch_step_items_skipped_total")
                    .tag("step_name", "syncGenresStep")
                    .counter();
            assertThat(skippedCounter).isNull();
        }

        @Test
        @DisplayName("should not record step duration when times are null")
        void shouldNotRecordStepDurationWhenTimesNull() {
            JobExecution jobExec = createJobExecution("importMoviesJob", BatchStatus.COMPLETED);
            StepExecution stepExec = new StepExecution("importMoviesStep", jobExec);
            stepExec.setStatus(BatchStatus.COMPLETED);
            stepExec.setReadCount(10);
            stepExec.setWriteCount(10);
            // startTime/endTime are null

            listener.afterStep(stepExec);

            // read/write counters should still exist
            assertThat(meterRegistry.find("batch_step_items_read_total")
                    .tag("step_name", "importMoviesStep").counter()).isNotNull();

            // but no duration timer
            Timer timer = meterRegistry.find("batch_step_duration_seconds")
                    .tag("step_name", "importMoviesStep")
                    .timer();
            assertThat(timer).isNull();
        }

        @Test
        @DisplayName("should return original exit status unchanged")
        void shouldReturnOriginalExitStatus() {
            JobExecution jobExec = createJobExecution("syncReviewsJob", BatchStatus.COMPLETED);
            StepExecution stepExec = createStepExecution(jobExec, "syncReviewsStep", 10, 10, 0, 0);
            stepExec.setExitStatus(ExitStatus.COMPLETED.addExitDescription("custom exit"));

            ExitStatus result = listener.afterStep(stepExec);

            assertThat(result.getExitCode()).isEqualTo("COMPLETED");
            assertThat(result.getExitDescription()).contains("custom exit");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multi-execution scenario
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("should accumulate metrics across multiple executions")
    void shouldAccumulateAcrossExecutions() {
        JobExecution job1 = createJobExecution("importMoviesJob", BatchStatus.COMPLETED);
        JobExecution job2 = createJobExecution("importMoviesJob", BatchStatus.COMPLETED);

        listener.afterJob(job1);
        listener.afterJob(job2);

        Counter counter = meterRegistry.find("batch_job_completed_total")
                .tag("job_name", "importMoviesJob")
                .tag("status", "COMPLETED")
                .counter();
        assertThat(counter.count()).isEqualTo(2.0);

        Timer timer = meterRegistry.find("batch_job_duration_seconds")
                .tag("job_name", "importMoviesJob")
                .timer();
        assertThat(timer.count()).isEqualTo(2);
    }
}
