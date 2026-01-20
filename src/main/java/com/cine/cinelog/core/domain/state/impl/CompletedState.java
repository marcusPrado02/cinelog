package com.cine.cinelog.core.domain.state.impl;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.core.domain.state.WatchEntryStatus;

import java.math.BigDecimal;

/**
 * Estado COMPLETED - Usuário terminou de assistir.
 *
 * <p>
 * Características deste estado:
 * <ul>
 * <li>Estado final (não permite mais transições)</li>
 * <li>Rating é recomendado (avaliação final)</li>
 * <li>Permite comentários (review completo)</li>
 * <li>watchedAt deve estar definido</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Permitidas</strong>:
 * <ul>
 * <li>❌ Nenhuma - Estado final</li>
 * </ul>
 *
 * <p>
 * <strong>Comportamento Especial</strong>:
 * <ul>
 * <li>Permite atualização de rating/comentário (reavaliação)</li>
 * <li>Não permite mudança de estado</li>
 * <li>Contribui para estatísticas e recomendações</li>
 * </ul>
 *
 * @since 1.0
 * @see WatchEntryStatus
 */
public class CompletedState implements WatchEntryStatus {

    @Override
    public WatchEntryStatusType getType() {
        return WatchEntryStatusType.COMPLETED;
    }

    @Override
    public WatchEntryStatus startWatching(WatchEntry entry) {
        // Transição inválida: estado final
        throw DomainException.of(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Não é possível modificar uma entrada já completada. Status: COMPLETED");
    }

    @Override
    public WatchEntryStatus complete(WatchEntry entry, BigDecimal rating) {
        // Transição inválida: já está completo
        // Mas permite atualizar rating (reavaliação)
        if (rating != null) {
            validateRating(rating);
            entry.setRating(rating);
        }
        return this; // Permanece no mesmo estado
    }

    @Override
    public WatchEntryStatus drop(WatchEntry entry, String reason) {
        // Transição inválida: estado final
        throw DomainException.of(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Não é possível abandonar uma mídia já completada. Status: COMPLETED");
    }

    @Override
    public void validateRating(BigDecimal rating) {
        // COMPLETED permite (e recomenda) rating
        if (rating != null) {
            if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.TEN) > 0) {
                throw DomainException.of(
                        ErrorCode.INVALID_RATING,
                        "Rating deve estar entre 0 e 10. Valor fornecido: " + rating);
            }
        }
        // Rating null é permitido (usuário pode não querer avaliar)
    }

    @Override
    public void validateComment(String comment) {
        // COMPLETED permite comentário (review completo)
        // Sem restrições adicionais
    }

    @Override
    public boolean isFinal() {
        return true; // COMPLETED é estado final
    }

    @Override
    public String getDescription() {
        return "Completado";
    }

    @Override
    public String toString() {
        return "CompletedState{type=" + getType() + ", description='" + getDescription() + "'}";
    }
}
