package com.cine.cinelog.features.batch.jobs.reports;

import com.cine.cinelog.features.reports.config.ReportProperties;
import com.cine.cinelog.features.reports.email.ReportEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class PlatformReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(PlatformReportTasklet.class);
    private final ReportEmailService reportEmailService;
    private final ReportProperties props;

    public PlatformReportTasklet(ReportEmailService reportEmailService, ReportProperties props) {
        this.reportEmailService = reportEmailService;
        this.props = props;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String adminEmail = System.getenv().getOrDefault("REPORT_ADMIN_EMAIL", props.getFromEmail());
        log.info("[BATCH] PlatformReportTasklet: enviando relatorio da plataforma para {}", adminEmail);
        reportEmailService.sendPlatformReportBlocking(adminEmail);
        log.info("[BATCH] PlatformReportTasklet: concluido");
        return RepeatStatus.FINISHED;
    }
}
