package com.cine.cinelog.core.domain.model;

/**
 * Enum representando os possíveis status de uma entrada de visualização.
 *
 * <p>
 * Estados possíveis do ciclo de vida de um WatchEntry:
 * <ul>
 * <li>PLANNING: Planejando assistir (na watchlist)</li>
 * <li>WATCHING: Assistindo atualmente</li>
 * <li>COMPLETED: Completado</li>
 * <li>DROPPED: Abandonado/desistiu</li>
 * </ul>
 *
 * <p>
 * Transições válidas (State Pattern):
 * 
 * <pre>
 * PLANNING → WATCHING → COMPLETED
 *     ↓         ↓
 *   DROPPED   DROPPED
 * </pre>
 *
 * @since 1.0
 * @see WatchEntryStatus
 */
public enum WatchEntryStatusType {
    /**
     * Usuário planeja assistir (ainda não começou).
     * Geralmente criado quando adiciona à watchlist.
     */
    PLANNING,

    /**
     * Usuário está assistindo atualmente.
     * Pode ter episódios parciais assistidos.
     */
    WATCHING,

    /**
     * Usuário completou a visualização.
     * Deve ter rating e pode ter comentário.
     */
    COMPLETED,

    /**
     * Usuário abandonou/desistiu.
     * Pode ter rating parcial e motivo do abandono.
     */
    DROPPED
}
