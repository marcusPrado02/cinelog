package com.cine.cinelog.features.watchprogress.repository;

import com.cine.cinelog.features.watchprogress.persistence.entity.WatchEntryProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA para {@link WatchEntryProgressEntity}.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * @since 1.0 (PR6 - Fase 5)
 */
@Repository
public interface WatchEntryProgressRepository extends JpaRepository<WatchEntryProgressEntity, Long> {

    /**
     * Busca progresso por ID do WatchEntry.
     *
     * @param watchEntryId ID do watch entry
     * @return progresso se existir
     */
    Optional<WatchEntryProgressEntity> findByWatchEntryId(Long watchEntryId);

    /**
     * Deleta progresso por ID do WatchEntry.
     *
     * @param watchEntryId ID do watch entry
     */
    void deleteByWatchEntryId(Long watchEntryId);

    /**
     * Verifica se existe progresso para um WatchEntry.
     *
     * @param watchEntryId ID do watch entry
     * @return true se existir
     */
    boolean existsByWatchEntryId(Long watchEntryId);
}
