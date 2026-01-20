package com.cine.cinelog.core.domain.state.impl;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.core.domain.state.WatchEntryStatus;

import java.math.BigDecimal;

/**
 * Estado PLANNING - Usuário planeja assistir (ainda não começou).
 *
 * <p>
 * Características deste estado:
 * <ul>
 * <li>Geralmente criado quando usuário adiciona mídia à watchlist</li>
 * <li>Não permite rating (ainda não assistiu)</li>
 * <li>Permite comentário (ex: "quero assistir porque...")</li>
 * <li>watchedAt deve ser null ou futura</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Permitidas</strong>:
 * <ul>
 * <li>→ WATCHING: quando usuário começa a assistir</li>
 * <li>→ DROPPED: quando usuário desiste antes de começar</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Bloqueadas</strong>:
 * <ul>
 * <li>❌ COMPLETED: não pode completar sem assistir</li>
 * </ul>
 *
 * @since 1.0
 * @see WatchEntryStatus
 */
public class PlanningState implements WatchEntryStatus {

    @Override
    public WatchEntryStatusType getType() {
        return WatchEntryStatusType.PLANNING;
    }

    @Override
    public WatchEntryStatus startWatching(WatchEntry entry) {
        // Transição válida: PLANNING → WATCHING
        return new WatchingState();
    }

    @Override
    public WatchEntryStatus complete(WatchEntry entry, BigDecimal rating) {
        // Transição inválida: PLANNING → COMPLETED
        // Não pode completar sem primeiro assistir
        throw DomainException.of(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Não é possível marcar como completo sem assistir. Status atual: PLANNING");
    }

    @Override
    public WatchEntryStatus drop(WatchEntry entry, String reason) {
        // Transição válida: PLANNING → DROPPED
        if (reason != null && !reason.isBlank()) {
            entry.setComment(reason.trim());
        }
        return new DroppedState();
    }

    @Override
    public void validateRating(BigDecimal rating) {
        // PLANNING não permite rating (ainda não assistiu)
        if (rating != null) {
            throw DomainException.of(
                    ErrorCode.INVALID_RATING,
                    "Não é possível avaliar uma mídia que ainda não foi assistida. Status: PLANNING");
        }
    }

    @Override
    public void validateComment(String comment) {
        // PLANNING permite comentário (ex: "quero assistir porque...")
        // Sem restrições adicionais
    }

    @Override
    public boolean isFinal() {
        return false; // PLANNING permite transições
    }

    @Override
    public String getDescription() {
        return "Planejando assistir";
    }

    @Override
    public String toString() {
        return "PlanningState{type=" + getType() + ", description='" + getDescription() + "'}";
    }
}
