package com.cine.cinelog.core.application.usecase.episodes;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.episodes.UpdateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por atualizar dados de episódios existentes.
 *
 * <p>
 * Permite alterar informações do episódio como nome, número, temporada
 * associada e data de exibição.
 *
 * @since 1.0
 * @see UpdateEpisodeUseCase
 * @see EpisodeRepositoryPort
 */
@Transactional
public class UpdateEpisodeService implements UpdateEpisodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateEpisodeService.class);

    private final EpisodeRepositoryPort repo;

    public UpdateEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a atualização de um episódio existente.
     *
     * @param id      o identificador único do episódio a ser atualizado
     * @param episode os novos dados do episódio
     * @return o episódio atualizado e persistido
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         episódio não existir
     */
    @Override
    @Observed(name = "episode.update", contextualName = "update-episode-service")
    @Measured("cinelog.service.episode.update")
    @AuditableAction(module = "EPISODE", action = "UPDATE", description = "Atualização de episódio")
    @Caching(evict = {
            @CacheEvict(value = "episodesPage", allEntries = true),
            @CacheEvict(value = "episodeById", key = "#id")
    })
    public Episode execute(Long id, Episode episode) {
        log.debug("Iniciando atualização de episódio. ID: {}", id);

        try {
            log.debug("Buscando episódio existente. ID: {}", id);
            Episode existing = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Episode not found: " + id));

            log.debug("Episódio encontrado. Aplicando atualizações");
            existing.setName(episode.getName());
            existing.setEpisodeNumber(episode.getEpisodeNumber());
            existing.setSeasonId(episode.getSeasonId());
            existing.setAirDate(episode.getAirDate());

            Episode saved = repo.save(existing);
            log.info("Episódio atualizado com sucesso. ID: {}, Nome: {}", id, saved.getName());
            return saved;
        } catch (DomainException e) {
            log.warn("Episódio não encontrado. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar episódio. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
