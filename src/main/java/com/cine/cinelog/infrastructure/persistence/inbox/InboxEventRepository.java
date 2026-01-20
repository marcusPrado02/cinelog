package com.cine.cinelog.infrastructure.persistence.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Repositório para gerenciar eventos recebidos via Kafka (inbox pattern).
 *
 * <p>
 * Suporta verificação de duplicatas e housekeeping de eventos antigos já
 * processados.
 * </p>
 */
@Repository
public interface InboxEventRepository extends JpaRepository<InboxEventEntity, UUID> {

    /**
     * Verifica se um evento já existe na inbox (para idempotência).
     *
     * @param eventId ID único do evento
     * @return true se já foi recebido/processado anteriormente
     */
    boolean existsByEventId(UUID eventId);

    /**
     * Remove eventos já processados que são mais antigos que uma data específica.
     *
     * <p>
     * Usado para housekeeping periódico da tabela inbox_event.
     * </p>
     *
     * @param threshold Timestamp de corte (eventos processados antes desta data
     *                  serão removidos)
     * @return Número de registros deletados
     */
    @Modifying
    @Query("DELETE FROM InboxEventEntity ie WHERE ie.processedAt IS NOT NULL AND ie.processedAt < :threshold")
    int deleteProcessedEventsBefore(@Param("threshold") Instant threshold);
}
