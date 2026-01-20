package com.cine.cinelog.core.domain.state.impl;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.core.domain.state.WatchEntryStatus;

import java.math.BigDecimal;

/**
 * Estado DROPPED - Usuário desistiu de assistir.
 *
 * <p>
 * Características deste estado:
 * <ul>
 * <li>Estado final (não permite mais transições)</li>
 * <li>Rating parcial permitido (impressão antes de desistir)</li>
 * <li>Comentário recomendado (motivo da desistência)</li>
 * <li>Pode ser alcançado de PLANNING ou WATCHING</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Permitidas</strong>:
 * <ul>
 * <li>❌ Nenhuma - Estado final</li>
 * </ul>
 *
 * <p>
 * <strong>Casos de Uso</strong>:
 * <ul>
 * <li>Desistiu antes de começar (PLANNING → DROPPED)</li>
 * <li>Desistiu no meio (WATCHING → DROPPED)</li>
 * <li>Não gostou e parou</li>
 * <li>Conteúdo inadequado</li>
 * </ul>
 *
 * @since 1.0
 * @see WatchEntryStatus
 */
public class DroppedState implements WatchEntryStatus {

    @Override
    public WatchEntryStatusType getType() {
        return WatchEntryStatusType.DROPPED;
    }

    @Override
    public WatchEntryStatus startWatching(WatchEntry entry) {
        // Transição inválida: estado final
        throw DomainException.of(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Não é possível retomar uma mídia abandonada. Crie uma nova entrada. Status: DROPPED");
    }

    @Override
    public WatchEntryStatus complete(WatchEntry entry, BigDecimal rating) {
        // Transição inválida: estado final
        throw DomainException.of(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Não é possível completar uma mídia abandonada. Status: DROPPED");
    }

    @Override
    public WatchEntryStatus drop(WatchEntry entry, String reason) {
        // Transição inválida: já está dropped
        // Mas permite atualizar motivo da desistência
        if (reason != null && !reason.isBlank()) {
            entry.setComment(reason.trim());
        }
        return this; // Permanece no mesmo estado
    }

    @Override
    public void validateRating(BigDecimal rating) {
        // DROPPED permite rating parcial (impressão antes de desistir)
        if (rating != null) {
            if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.TEN) > 0) {
                throw DomainException.of(
                        ErrorCode.INVALID_RATING,
                        "Rating deve estar entre 0 e 10. Valor fornecido: " + rating);
            }
        }
        // Rating null é permitido (pode ter desistido antes de formar opinião)
    }

    @Override
    public void validateComment(String comment) {
        // DROPPED permite (e recomenda) comentário explicando motivo
        // Sem restrições adicionais
    }

    @Override
    public boolean isFinal() {
        return true; // DROPPED é estado final
    }

    @Override
    public String getDescription() {
        return "Abandonado";
    }

    @Override
    public String toString() {
        return "DroppedState{type=" + getType() + ", description='" + getDescription() + "'}";
    }
}
