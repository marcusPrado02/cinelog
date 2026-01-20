package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Job de housekeeping para limpar eventos antigos já processados da tabela
 * inbox_event.
 *
 * <p>
 * Remove eventos processados com mais de 7 dias para evitar crescimento
 * ilimitado da tabela.
 * </p>
 *
 * <p>
 * <strong>Execução:</strong>
 * </p>
 * <ul>
 * <li>Agendado via @Scheduled (configurável por propriedade)</li>
 * <li>Padrão: executa diariamente à 03:00 (cron)</li>
 * <li>Pode ser desabilitado via propriedade:
 * inbox.housekeeping.enabled=false</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "inbox.housekeeping", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InboxHousekeepingJob {

    private static final Logger log = LoggerFactory.getLogger(InboxHousekeepingJob.class);

    private final InboxEventRepository inboxRepository;

    public InboxHousekeepingJob(InboxEventRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    /**
     * Executa housekeeping da inbox: remove eventos processados antigos.
     *
     * <p>
     * Padrão: remove eventos processados há mais de 7 dias.
     * </p>
     * <p>
     * Agendamento: diariamente às 03:00 (timezone do servidor).
     * </p>
     */
    @Scheduled(cron = "${inbox.housekeeping.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupOldProcessedEvents() {
        try {
            log.info("Iniciando housekeeping da inbox_event...");

            // Configura threshold: 7 dias atrás
            int retentionDays = 7; // Pode ser configurável via @Value se necessário
            Instant threshold = Instant.now().minus(Duration.ofDays(retentionDays));

            int deletedCount = inboxRepository.deleteProcessedEventsBefore(threshold);

            log.info("Housekeeping da inbox_event concluído: {} eventos removidos (processados antes de {})",
                    deletedCount, threshold);

        } catch (Exception ex) {
            log.error("Erro durante housekeeping da inbox_event: {}", ex.getMessage(), ex);
            // Não propaga exceção para não interromper scheduler
        }
    }
}
