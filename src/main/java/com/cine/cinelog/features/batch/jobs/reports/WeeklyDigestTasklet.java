package com.cine.cinelog.features.batch.jobs.reports;

import com.cine.cinelog.features.reports.email.ReportEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class WeeklyDigestTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestTasklet.class);
    private final ReportEmailService reportEmailService;

    public WeeklyDigestTasklet(ReportEmailService reportEmailService) {
        this.reportEmailService = reportEmailService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("[BATCH] WeeklyDigestTasklet: enviando digest semanal para todos os usuarios");
        reportEmailService.sendWeeklyDigestToAllBlocking();
        log.info("[BATCH] WeeklyDigestTasklet: concluido");
        return RepeatStatus.FINISHED;
    }
}
