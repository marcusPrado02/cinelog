package com.cine.cinelog.core.domain.state;

import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.core.domain.state.impl.CompletedState;
import com.cine.cinelog.core.domain.state.impl.DroppedState;
import com.cine.cinelog.core.domain.state.impl.PlanningState;
import com.cine.cinelog.core.domain.state.impl.WatchingState;

/**
 * Factory para criação de objetos WatchEntryStatus baseado no tipo.
 *
 * <p>
 * Esta factory implementa o padrão Factory Method para encapsular
 * a lógica de criação de estados concretos do State Pattern.
 *
 * <p>
 * <strong>Uso Principal</strong>:
 * <ul>
 * <li>Reconstrução de estado após load do banco (JPA @PostLoad)</li>
 * <li>Testes unitários (criar estados isoladamente)</li>
 * <li>Inicialização de novas entradas (estado PLANNING)</li>
 * </ul>
 *
 * <p>
 * <strong>Exemplo de Uso</strong>:
 * 
 * <pre>{@code
 * // Reconstruir estado após load do banco
 * &#64;PostLoad
 * public void reconstructState() {
 *     this.status = WatchEntryStatusFactory.create(this.statusType);
 * }
 *
 * // Criar nova entrada (sempre começa em PLANNING)
 * WatchEntry entry = new WatchEntry(user, media);
 * entry.setStatus(WatchEntryStatusFactory.createInitial());
 * }</pre>
 *
 * @since 1.0
 * @see WatchEntryStatus
 * @see WatchEntryStatusType
 */
public final class WatchEntryStatusFactory {

    /**
     * Construtor privado para prevenir instanciação.
     * Esta é uma classe utilitária com apenas métodos estáticos.
     */
    private WatchEntryStatusFactory() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * Cria uma instância de WatchEntryStatus baseado no tipo fornecido.
     *
     * @param type o tipo de status a ser criado (não pode ser null)
     * @return instância concreta do estado correspondente
     * @throws IllegalArgumentException se type for null
     * @throws IllegalStateException    se o tipo não for reconhecido
     */
    public static WatchEntryStatus create(WatchEntryStatusType type) {
        if (type == null) {
            throw new IllegalArgumentException("WatchEntryStatusType cannot be null");
        }

        return switch (type) {
            case PLANNING -> new PlanningState();
            case WATCHING -> new WatchingState();
            case COMPLETED -> new CompletedState();
            case DROPPED -> new DroppedState();
        };
    }

    /**
     * Cria o estado inicial padrão para novas entradas.
     *
     * <p>
     * Todas as novas entradas de WatchEntry começam no estado PLANNING
     * (planejando assistir), que é o estado inicial do ciclo de vida.
     *
     * @return nova instância de PlanningState
     */
    public static WatchEntryStatus createInitial() {
        return new PlanningState();
    }

    /**
     * Verifica se um tipo de status representa um estado final.
     *
     * <p>
     * Estados finais não permitem transições para outros estados
     * (exceto operações especiais como reavaliação).
     *
     * @param type o tipo a verificar
     * @return true se for COMPLETED ou DROPPED, false caso contrário
     */
    public static boolean isFinalState(WatchEntryStatusType type) {
        if (type == null) {
            return false;
        }
        return type == WatchEntryStatusType.COMPLETED || type == WatchEntryStatusType.DROPPED;
    }

    /**
     * Obtém descrição amigável do estado.
     *
     * @param type o tipo de status
     * @return descrição do estado em português
     */
    public static String getDescription(WatchEntryStatusType type) {
        if (type == null) {
            return "Estado desconhecido";
        }

        WatchEntryStatus status = create(type);
        return status.getDescription();
    }
}
