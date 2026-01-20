package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.WatchlistItem;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de persistência relacionadas à watchlist (lista
 * de desejos).
 * 
 * <p>
 * Define o contrato para gerenciar itens da watchlist dos usuários, permitindo:
 * <ul>
 * <li>Salvar novos itens</li>
 * <li>Buscar itens por usuário e mídia</li>
 * <li>Listar todos os itens de um usuário</li>
 * <li>Remover itens</li>
 * <li>Verificar existência de associações</li>
 * </ul>
 * 
 * <p>
 * Esta interface segue o padrão de Arquitetura Hexagonal,
 * isolando a lógica de domínio das implementações de infraestrutura.
 * 
 * @since 1.0
 * @see WatchlistItem
 */
public interface WatchlistRepositoryPort {

    /**
     * Salva um item da watchlist no repositório.
     * 
     * @param item o item da watchlist a ser salvo
     * @return o item salvo com ID gerado
     */
    WatchlistItem save(WatchlistItem item);

    /**
     * Busca um item da watchlist por usuário e mídia.
     * 
     * @param userId  o identificador do usuário
     * @param mediaId o identificador da mídia
     * @return Optional contendo o item se encontrado
     */
    Optional<WatchlistItem> findByUserIdAndMediaId(Long userId, Long mediaId);

    /**
     * Lista todos os itens da watchlist de um usuário.
     * 
     * @param userId o identificador do usuário
     * @return resultado paginado com os itens da watchlist
     */
    PageResult<WatchlistItem> findAllByUserId(Long userId);

    /**
     * Remove um item da watchlist por ID.
     * 
     * @param id o identificador do item a ser removido
     */
    void deleteById(Long id);

    /**
     * Verifica se existe algum item na watchlist para a mídia especificada.
     * 
     * @param mediaId o identificador da mídia
     * @return true se houver pelo menos um item, false caso contrário
     */
    boolean existsByMediaId(Long mediaId);

    /**
     * Verifica se existe algum item na watchlist do usuário especificado.
     * 
     * @param userId o identificador do usuário
     * @return true se o usuário tiver itens na watchlist, false caso contrário
     */
    boolean existsByUserId(Long userId);
}