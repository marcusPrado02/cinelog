package com.cine.cinelog.features.reports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the email reports feature.
 *
 * <p>
 * Bound from {@code cinelog.reports.*} in application.yml.
 * </p>
 *
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "cinelog.reports")
public class ReportProperties {

    private boolean enabled = true;
    private String fromEmail = "noreply@cinelog.dev";
    private String fromName = "CineLog";
    private String baseUrl = "http://localhost:8080";
    private Cron cron = new Cron();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Cron getCron() {
        return cron;
    }

    public void setCron(Cron cron) {
        this.cron = cron;
    }

    public static class Cron {
        private String weeklyDigest = "0 0 8 * * MON";
        private String trending = "0 0 18 * * FRI";
        private String platformReport = "0 0 6 * * SUN";

        public String getWeeklyDigest() {
            return weeklyDigest;
        }

        public void setWeeklyDigest(String weeklyDigest) {
            this.weeklyDigest = weeklyDigest;
        }

        public String getTrending() {
            return trending;
        }

        public void setTrending(String trending) {
            this.trending = trending;
        }

        public String getPlatformReport() {
            return platformReport;
        }

        public void setPlatformReport(String platformReport) {
            this.platformReport = platformReport;
        }
    }
}
