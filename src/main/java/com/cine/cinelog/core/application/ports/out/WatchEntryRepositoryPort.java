package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Porta de saída para operações de repositório relacionadas a WatchEntry.
 * Define as operações CRUD básicas para gerenciar entidades WatchEntry.
 */
public interface WatchEntryRepositoryPort {
    /**
     * Salva uma entrada de visualização no repositório.
     *
     * @param entry O objeto WatchEntry a ser salvo.
     * @return A entrada de visualização salva.
     */
    WatchEntry save(WatchEntry entry);

    /**
     * Recupera uma entrada de visualização existente do repositório.
     *
     * @param id O ID da entrada de visualização a ser recuperada.
     * @return Um Optional contendo a entrada de visualização encontrada, ou vazio
     *         se não
     *         encontrado.
     */
    Optional<WatchEntry> findById(Long id);

    /**
     * Remove uma entrada de visualização existente do repositório.
     *
     * @param id O ID da entrada de visualização a ser removida.
     */
    void deleteById(Long id);

    /**
     * Lista as entradas de visualização com base nos filtros fornecidos.
     *
     * @param userId    O ID do usuário.
     * @param mediaId   O ID da mídia (opcional).
     * @param episodeId O ID do episódio (opcional).
     * @param minRating A classificação mínima (opcional).
     * @param from      A data inicial do intervalo (opcional).
     * @param to        A data final do intervalo (opcional).
     * @param pageable  As informações de paginação.
     * @return Uma página de entradas de visualização que correspondem aos
     *         critérios fornecidos.
     */
    Page<WatchEntry> listByUser(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable);
}