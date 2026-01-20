package com.cine.cinelog.infrastructure.events;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações para o OutboxPublisherJob.
 *
 * @since 1.1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "outbox.publisher")
public class OutboxPublisherConfig {

    /**
     * Se o publisher está habilitado.
     */
    private boolean enabled = true;

    /**
     * Taxa fixa de execução em milissegundos.
     */
    private long fixedRateMs = 5000;

    /**
     * Delay inicial antes da primeira execução (ms).
     */
    private long initialDelayMs = 10000;

    /**
     * Tamanho do batch de eventos a processar por vez.
     */
    private int batchSize = 100;

    /**
     * Dias de retenção para eventos já processados.
     * Eventos SENT mais antigos são removidos.
     */
    private int retentionDays = 7;
}
