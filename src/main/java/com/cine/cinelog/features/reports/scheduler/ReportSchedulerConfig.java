package com.cine.cinelog.features.reports.scheduler;

import com.cine.cinelog.features.reports.config.ReportProperties;
import com.cine.cinelog.features.reports.email.ReportEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled task configuration for automatic report delivery.
 *
 * <p>All three jobs are enabled only when {@code cinelog.reports.enabled=true}
 * (default) so they can be disabled in test or CI environments.</p>
 *
 * <p>Cron expressions are configured in {@code application.yml}:</p>
 * <ul>
 *   <li>Weekly digest → Monday 08:00</li>
 *   <li>Trending       → Friday  18:00</li>
 *   <li>Platform report → Sunday  06:00</li>
 * </ul>
 *
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(prefix = "cinelog.reports", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportSchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(ReportSchedulerConfig.class);

    private final ReportEmailService reportEmailService;
    private final ReportProperties props;

    public ReportSchedulerConfig(ReportEmailService reportEmailService,
                                  ReportProperties props) {
        this.reportEmailService = reportEmailService;
        this.props = props;
    }

    /**
     * Sends the weekly activity digest to all users every Monday at 08:00.
     */
    @Scheduled(cron = "${cinelog.reports.cron.weekly-digest:0 0 8 * * MON}")
    public void weeklyDigestJob() {
        log.info("[ReportScheduler] Starting weekly digest job");
        reportEmailService.sendWeeklyDigestToAll();
        log.info("[ReportScheduler] Weekly digest job dispatched");
    }

    /**
     * Sends the trending report to all users every Friday at 18:00.
     */
    @Scheduled(cron = "${cinelog.reports.cron.trending:0 0 18 * * FRI}")
    public void trendingJob() {
        log.info("[ReportScheduler] Starting trending report job");
        reportEmailService.sendTrendingToAll();
        log.info("[ReportScheduler] Trending report job dispatched");
    }

    /**
     * Sends the admin platform report to the configured admin email every Sunday at 06:00.
     *
     * <p>Uses the {@code REPORT_ADMIN_EMAIL} env var, defaulting to the {@code from-email}.</p>
     */
    @Scheduled(cron = "${cinelog.reports.cron.platform-report:0 0 6 * * SUN}")
    public void platformReportJob() {
        String adminEmail = System.getenv().getOrDefault("REPORT_ADMIN_EMAIL", props.getFromEmail());
        log.info("[ReportScheduler] Starting platform report job → {}", adminEmail);
        reportEmailService.sendPlatformReport(adminEmail);
        log.info("[ReportScheduler] Platform report job dispatched");
    }
}
