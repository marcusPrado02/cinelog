package com.cine.cinelog.core.application.ports.in.watchentry;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cine.cinelog.core.domain.model.WatchEntry;

/**
 * Caso de uso para listar todas as entradas do histórico (WatchEntry).
 * Essa interface define o contrato da aplicação sem dependências de
 * infraestrutura.
 */
public interface ListWatchEntriesUseCase {

    /**
     * Lista todas as entradas do histórico (WatchEntry) existentes.
     *
     * @return lista de WatchEntry
     */
    Page<WatchEntry> execute(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable);

}