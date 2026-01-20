package com.cine.cinelog.features.watchlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cine.cinelog.features.watchlist.persistence.WatchlistItemEntity;

/**
 * Repositório JPA para gerenciamento de itens da watchlist (lista de desejos).
 * 
 * <p>
 * Fornece operações de persistência para {@link WatchlistItemEntity},
 * incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca por usuário e mídia</li>
 * <li>Listagem ordenada por data de adição</li>
 * <li>Verificação de existência</li>
 * </ul>
 * 
 * @since 1.0
 * @see WatchlistItemEntity
 */
public interface WatchlistJpaRepository extends JpaRepository<WatchlistItemEntity, Long> {

    /**
     * Busca um item da watchlist por usuário e mídia.
     * 
     * @param userId  o identificador do usuário
     * @param mediaId o identificador da mídia
     * @return Optional contendo o item se encontrado
     */
    Optional<WatchlistItemEntity> findByUserIdAndMediaId(Long userId, Long mediaId);

    /**
     * Lista todos os itens da watchlist de um usuário, ordenados por data de adição
     * (mais recentes primeiro).
     * 
     * @param userId o identificador do usuário
     * @return lista de itens da watchlist
     */
    List<WatchlistItemEntity> findAllByUserIdOrderByAddedAtDesc(Long userId);

    /**
     * Verifica se existe algum item na watchlist para a mídia especificada.
     * 
     * @param mediaId o identificador da mídia
     * @return true se houver pelo menos um item, false caso contrário
     */
    boolean existsByMediaId(Long mediaId);

    /**
     * Verifica se o usuário possui algum item na watchlist.
     * 
     * @param userId o identificador do usuário
     * @return true se o usuário tiver itens, false caso contrário
     */
    boolean existsByUserId(Long userId);
}
