package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.UserStats;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        PageResult<WatchEntry> listByUser(Long userId, Long mediaId, Long episodeId, Integer minRating,
                        LocalDate from, LocalDate to, Pageable pageable);

        /**
         * Calcula estatísticas de visualização para um usuário específico.
         * 
         * @param userId O ID do usuário.
         * @return As estatísticas de visualização do usuário.
         */
        UserStats computeStatsForUser(Long userId);

        /**
         * Verifica se uma entrada de visualização existe para uma mídia específica.
         *
         * @param mediaId O ID da mídia.
         * @return true se existir, false caso contrário.
         */
        boolean existsByMediaId(Long mediaId);

        /**
         * Verifica se uma entrada de visualização existe para um usuário, mídia,
         * episódio e data específicos.
         *
         * @param userId    O ID do usuário.
         * @param mediaId   O ID da mídia.
         * @param episodeId O ID do episódio.
         * @param watchedAt A data em que o episódio foi assistido.
         * @return true se existir, false caso contrário.
         */
        boolean existsByUserMediaEpisodeAndDate(
                        Long userId,
                        Long mediaId,
                        Long episodeId,
                        LocalDate watchedAt);

        /**
         * Verifica se existem entradas de visualização para um usuário específico.
         *
         * @param userId O ID do usuário.
         * @return true se existirem entradas de visualização, false caso contrário.
         */
        boolean existsByUserId(Long userId);

        /**
         * Verifica se existem entradas de visualização para um episódio específico.
         *
         * @param episodeId O ID do episódio.
         * @return true se existirem entradas de visualização, false caso contrário.
         */
        boolean existsByEpisodeId(Long episodeId);

        /**
         * Retorna total de entradas para o usuário.
         * 
         * @param userId O ID do usuário.
         * @return total de entradas.
         */
        long countEntriesByUserId(Long userId);

        /**
         * Retorna total de entradas com rating (onde rating != null).
         * 
         * @param userId O ID do usuário.
         * @return total de entradas com rating.
         */
        long countRatedEntriesByUserId(Long userId);

        /**
         * Retorna média dos ratings do usuário, se existir ao menos um.
         * 
         * @param userId O ID do usuário.
         * @return média dos ratings, ou vazio se não houver ratings.
         */
        Optional<Double> averageRatingByUserId(Long userId);

        /**
         * Retorna a data da primeira visualização do usuário.
         * 
         * @param userId O ID do usuário.
         * @return A data da primeira visualização, ou null se não houver.
         */
        LocalDate findFirstWatchDateByUserId(Long userId);

        /**
         * Retorna a data da última visualização do usuário.
         *
         * @param userId O ID do usuário.
         * @return A data da última visualização, ou null se não houver.
         */
        LocalDate findLastWatchDateByUserId(Long userId);
}