package com.cine.cinelog.core.domain.state;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;

import java.math.BigDecimal;

/**
 * State Pattern - Interface para estados de WatchEntry.
 *
 * <p>
 * Cada estado define seu próprio comportamento e transições válidas.
 * Permite que WatchEntry mude comportamento dinamicamente baseado em seu estado
 * interno.
 *
 * <p>
 * <strong>Benefícios do Pattern</strong>:
 * <ul>
 * <li>Encapsula comportamento específico de cada estado</li>
 * <li>Elimina conditionals complexos (if/switch de status)</li>
 * <li>Single Responsibility: cada estado gerencia suas próprias regras</li>
 * <li>Open/Closed: novos estados podem ser adicionados sem modificar código
 * existente</li>
 * <li>Facilita testes: cada estado testado isoladamente</li>
 * </ul>
 *
 * <p>
 * <strong>Transições Permitidas</strong>:
 * 
 * <pre>
 * PLANNING → WATCHING (quando começa a assistir)
 * PLANNING → DROPPED (desistiu antes de começar)
 * WATCHING → COMPLETED (quando termina)
 * WATCHING → DROPPED (desistiu no meio)
 * COMPLETED → (final - não permite transição)
 * DROPPED → (final - não permite transição)
 * </pre>
 *
 * @since 1.0
 * @see PlanningState
 * @see WatchingState
 * @see CompletedState
 * @see DroppedState
 */
public interface WatchEntryStatus {

    /**
     * Retorna o tipo do status.
     */
    WatchEntryStatusType getType();

    /**
     * Tenta transicionar para o estado WATCHING.
     *
     * @param entry o watch entry a ser atualizado
     * @return novo estado WATCHING
     * @throws DomainException se a transição não é válida neste estado
     */
    WatchEntryStatus startWatching(WatchEntry entry);

    /**
     * Tenta transicionar para o estado COMPLETED.
     *
     * @param entry  o watch entry a ser atualizado
     * @param rating avaliação final (obrigatório para COMPLETED)
     * @return novo estado COMPLETED
     * @throws DomainException se a transição não é válida ou rating inválido
     */
    WatchEntryStatus complete(WatchEntry entry, BigDecimal rating);

    /**
     * Tenta transicionar para o estado DROPPED.
     *
     * @param entry  o watch entry a ser atualizado
     * @param reason motivo do abandono (opcional)
     * @return novo estado DROPPED
     * @throws DomainException se a transição não é válida
     */
    WatchEntryStatus drop(WatchEntry entry, String reason);

    /**
     * Valida se pode aplicar rating neste estado.
     *
     * @param rating o rating a ser aplicado
     * @throws DomainException se rating não é permitido neste estado
     */
    void validateRating(BigDecimal rating);

    /**
     * Valida se pode adicionar comentário neste estado.
     *
     * @param comment o comentário a ser adicionado
     * @throws DomainException se comentário não é permitido neste estado
     */
    void validateComment(String comment);

    /**
     * Indica se este é um estado final (não permite mais transições).
     */
    boolean isFinal();

    /**
     * Retorna descrição human-readable do estado.
     */
    String getDescription();
}
