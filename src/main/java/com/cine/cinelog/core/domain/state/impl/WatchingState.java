package com.cine.cinelog.core.domain.state.impl;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.core.domain.state.WatchEntryStatus;

import java.math.BigDecimal;

/**
 * Estado WATCHING - Usuário está assistindo atualmente.
 *
 * <p>
 * Características deste estado:
 * <ul>
 * <li>Representa visualização ativa</li>
 * <li>Pode ter rating parcial (impressões iniciais)</li>
 * <li>Permite comentários (ex: "estou no episódio 5...")</li>
 * <li>watchedAt pode ser progressivo</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Permitidas</strong>:
 * <ul>
 * <li>→ COMPLETED: quando termina de assistir</li>
 * <li>→ DROPPED: quando desiste no meio</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Bloqueadas</strong>:
 * <ul>
 * <li>❌ PLANNING: não pode voltar a planejar</li>
 * <li>❌ WATCHING: já está assistindo</li>
 * </ul>
 *
 * @since 1.0
 * @see WatchEntryStatus
 */
public class WatchingState implements WatchEntryStatus {

    @Override
    public WatchEntryStatusType getType() {
        return WatchEntryStatusType.WATCHING;
    }

    @Override
    public WatchEntryStatus startWatching(WatchEntry entry) {
        // Transição inválida: WATCHING → WATCHING
        // Já está assistindo
        throw DomainException.of(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Já está assistindo esta mídia. Status atual: WATCHING");
    }

    @Override
    public WatchEntryStatus complete(WatchEntry entry, BigDecimal rating) {
        // Transição válida: WATCHING → COMPLETED
        if (rating != null) {
            validateRating(rating);
            entry.setRating(rating);
        }
        return new CompletedState();
    }

    @Override
    public WatchEntryStatus drop(WatchEntry entry, String reason) {
        // Transição válida: WATCHING → DROPPED
        if (reason != null && !reason.isBlank()) {
            entry.setComment(reason.trim());
        }
        return new DroppedState();
    }

    @Override
    public void validateRating(BigDecimal rating) {
        // WATCHING permite rating (impressões parciais)
        if (rating != null) {
            if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.TEN) > 0) {
                throw DomainException.of(
                        ErrorCode.INVALID_RATING,
                        "Rating deve estar entre 0 e 10. Valor fornecido: " + rating);
            }
        }
    }

    @Override
    public void validateComment(String comment) {
        // WATCHING permite comentário (ex: "estou no episódio 5...")
        // Sem restrições adicionais
    }

    @Override
    public boolean isFinal() {
        return false; // WATCHING permite transições
    }

    @Override
    public String getDescription() {
        return "Assistindo";
    }

    @Override
    public String toString() {
        return "WatchingState{type=" + getType() + ", description='" + getDescription() + "'}";
    }
}
