package com.cine.cinelog.features.reports.email;

import com.cine.cinelog.features.reports.config.ReportProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Low-level email sending service that wraps JavaMailSender and Thymeleaf.
 *
 * <p>All send operations are {@code @Async} — they execute in a separate thread
 * so that the calling request returns immediately with 202 Accepted.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ReportProperties props;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        ReportProperties props) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.props = props;
    }

    /**
     * Sends an HTML email rendered from a Thymeleaf template.
     *
     * @param to           recipient email address
     * @param subject      email subject
     * @param templateName template path under {@code templates/email/} (without extension)
     * @param variables    variables to pass to the template context
     */
    public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context ctx = new Context();
            variables.forEach(ctx::setVariable);
            ctx.setVariable("baseUrl", props.getBaseUrl());
            ctx.setVariable("appName", props.getFromName());

            String html = templateEngine.process("email/" + templateName, ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(props.getFromEmail(), props.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(msg);
            log.info("Email sent: to={} subject={}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
