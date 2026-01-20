package com.cine.cinelog.core.application.services;

import com.cine.cinelog.core.domain.vo.SeriesProgress;
import com.cine.cinelog.features.watchprogress.persistence.entity.WatchEntryProgressEntity;
import com.cine.cinelog.features.watchprogress.repository.WatchEntryProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * Serviço para gerenciamento de progresso de visualização de séries.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * <p>
 * Funcionalidades:
 * <ul>
 * <li>Criar/Atualizar progresso de episódio</li>
 * <li>Buscar progresso atual</li>
 * <li>Avançar para próximo episódio/temporada</li>
 * <li>Deletar progresso (quando série é completada/dropped)</li>
 * </ul>
 *
 * <p>
 * <strong>Cache:</strong> Progresso é cacheado por 5 minutos para reduzir
 * queries.
 * Cache invalidado em atualizações/deleções.
 *
 * @since 1.0 (PR6 - Fase 5)
 */
@Service
@Transactional
public class WatchProgressService {

    private static final Logger log = LoggerFactory.getLogger(WatchProgressService.class);

    private final WatchEntryProgressRepository progressRepository;

    public WatchProgressService(WatchEntryProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    /**
     * Cria ou atualiza progresso de um watch entry.
     *
     * @param watchEntryId ID do watch entry (deve ser episódio)
     * @param progress     progresso a salvar
     * @return progresso salvo
     */
    @CacheEvict(value = "watchProgress", key = "#watchEntryId")
    public SeriesProgress saveProgress(Long watchEntryId, SeriesProgress progress) {
        log.debug("Salvando progresso para watchEntry={}: {}", watchEntryId, progress);

        WatchEntryProgressEntity entity = progressRepository.findByWatchEntryId(watchEntryId)
                .orElseGet(() -> {
                    WatchEntryProgressEntity newEntity = new WatchEntryProgressEntity();
                    newEntity.setWatchEntryId(watchEntryId);
                    return newEntity;
                });

        // Mapear de VO para Entity
        entity.setCurrentSeason(progress.getCurrentSeason());
        entity.setCurrentEpisode(progress.getCurrentEpisode());
        entity.setWatchedDurationSeconds(progress.getWatchedDuration().toSeconds());
        entity.setTotalDurationSeconds(progress.getTotalDuration().toSeconds());

        WatchEntryProgressEntity saved = progressRepository.save(entity);
        log.debug("Progresso salvo: id={}, watchEntry={}", saved.getId(), watchEntryId);

        return toValueObject(saved);
    }

    /**
     * Busca progresso de um watch entry.
     *
     * @param watchEntryId ID do watch entry
     * @return progresso se existir
     */
    @Cacheable(value = "watchProgress", key = "#watchEntryId")
    @Transactional(readOnly = true)
    public Optional<SeriesProgress> getProgress(Long watchEntryId) {
        log.debug("Buscando progresso para watchEntry={}", watchEntryId);

        return progressRepository.findByWatchEntryId(watchEntryId)
                .map(this::toValueObject);
    }

    /**
     * Atualiza apenas o tempo assistido do episódio atual.
     *
     * @param watchEntryId    ID do watch entry
     * @param watchedDuration novo tempo assistido
     * @return progresso atualizado
     */
    @CacheEvict(value = "watchProgress", key = "#watchEntryId")
    public Optional<SeriesProgress> updateWatchedTime(Long watchEntryId, Duration watchedDuration) {
        log.debug("Atualizando tempo assistido: watchEntry={}, duration={}", watchEntryId, watchedDuration);

        Optional<WatchEntryProgressEntity> entityOpt = progressRepository.findByWatchEntryId(watchEntryId);

        if (entityOpt.isEmpty()) {
            log.warn("Progresso não encontrado para watchEntry={}", watchEntryId);
            return Optional.empty();
        }

        WatchEntryProgressEntity entity = entityOpt.get();
        SeriesProgress currentProgress = toValueObject(entity);
        SeriesProgress updatedProgress = currentProgress.updateWatchedTime(watchedDuration);

        entity.setWatchedDurationSeconds(updatedProgress.getWatchedDuration().toSeconds());
        progressRepository.save(entity);

        log.debug("Tempo assistido atualizado: watchEntry={}, novo progresso={}", watchEntryId, updatedProgress);

        return Optional.of(updatedProgress);
    }

    /**
     * Avança para próximo episódio (mesmo season).
     *
     * @param watchEntryId        ID do watch entry
     * @param nextEpisodeDuration duração do próximo episódio
     * @return progresso atualizado
     */
    @CacheEvict(value = "watchProgress", key = "#watchEntryId")
    public Optional<SeriesProgress> advanceToNextEpisode(Long watchEntryId, Duration nextEpisodeDuration) {
        log.debug("Avançando para próximo episódio: watchEntry={}, duration={}", watchEntryId, nextEpisodeDuration);

        Optional<WatchEntryProgressEntity> entityOpt = progressRepository.findByWatchEntryId(watchEntryId);

        if (entityOpt.isEmpty()) {
            log.warn("Progresso não encontrado para watchEntry={}", watchEntryId);
            return Optional.empty();
        }

        WatchEntryProgressEntity entity = entityOpt.get();
        SeriesProgress currentProgress = toValueObject(entity);
        SeriesProgress nextProgress = currentProgress.nextEpisode(nextEpisodeDuration);

        entity.setCurrentEpisode(nextProgress.getCurrentEpisode());
        entity.setWatchedDurationSeconds(nextProgress.getWatchedDuration().toSeconds());
        entity.setTotalDurationSeconds(nextProgress.getTotalDuration().toSeconds());
        progressRepository.save(entity);

        log.info("Avançado para próximo episódio: watchEntry={}, novo progresso={}", watchEntryId, nextProgress);

        return Optional.of(nextProgress);
    }

    /**
     * Avança para próxima temporada (episódio 1).
     *
     * @param watchEntryId         ID do watch entry
     * @param firstEpisodeDuration duração do primeiro episódio da nova temporada
     * @return progresso atualizado
     */
    @CacheEvict(value = "watchProgress", key = "#watchEntryId")
    public Optional<SeriesProgress> advanceToNextSeason(Long watchEntryId, Duration firstEpisodeDuration) {
        log.debug("Avançando para próxima temporada: watchEntry={}, duration={}", watchEntryId, firstEpisodeDuration);

        Optional<WatchEntryProgressEntity> entityOpt = progressRepository.findByWatchEntryId(watchEntryId);

        if (entityOpt.isEmpty()) {
            log.warn("Progresso não encontrado para watchEntry={}", watchEntryId);
            return Optional.empty();
        }

        WatchEntryProgressEntity entity = entityOpt.get();
        SeriesProgress currentProgress = toValueObject(entity);
        SeriesProgress nextProgress = currentProgress.nextSeason(firstEpisodeDuration);

        entity.setCurrentSeason(nextProgress.getCurrentSeason());
        entity.setCurrentEpisode(nextProgress.getCurrentEpisode());
        entity.setWatchedDurationSeconds(nextProgress.getWatchedDuration().toSeconds());
        entity.setTotalDurationSeconds(nextProgress.getTotalDuration().toSeconds());
        progressRepository.save(entity);

        log.info("Avançado para próxima temporada: watchEntry={}, novo progresso={}", watchEntryId, nextProgress);

        return Optional.of(nextProgress);
    }

    /**
     * Deleta progresso de um watch entry.
     * Usado quando série é completada/dropped.
     *
     * @param watchEntryId ID do watch entry
     */
    @CacheEvict(value = "watchProgress", key = "#watchEntryId")
    public void deleteProgress(Long watchEntryId) {
        log.debug("Deletando progresso para watchEntry={}", watchEntryId);

        if (progressRepository.existsByWatchEntryId(watchEntryId)) {
            progressRepository.deleteByWatchEntryId(watchEntryId);
            log.info("Progresso deletado para watchEntry={}", watchEntryId);
        } else {
            log.debug("Nenhum progresso encontrado para deletar: watchEntry={}", watchEntryId);
        }
    }

    /**
     * Verifica se existe progresso para um watch entry.
     *
     * @param watchEntryId ID do watch entry
     * @return true se existir
     */
    @Transactional(readOnly = true)
    public boolean hasProgress(Long watchEntryId) {
        return progressRepository.existsByWatchEntryId(watchEntryId);
    }

    /**
     * Converte Entity para Value Object.
     */
    private SeriesProgress toValueObject(WatchEntryProgressEntity entity) {
        return new SeriesProgress(
                entity.getCurrentSeason(),
                entity.getCurrentEpisode(),
                Duration.ofSeconds(entity.getWatchedDurationSeconds()),
                Duration.ofSeconds(entity.getTotalDurationSeconds()));
    }
}
