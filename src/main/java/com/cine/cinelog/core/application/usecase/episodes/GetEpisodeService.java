package com.cine.cinelog.core.application.usecase.episodes;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.episodes.GetEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar um episódio específico por seu identificador.
 * 
 * <p>
 * Recupera os dados completos de um episódio, incluindo número, nome e data de
 * exibição.
 * Utiliza cache para otimizar performance em consultas repetidas.
 * 
 * @since 1.0
 * @see GetEpisodeUseCase
 * @see EpisodeRepositoryPort
 */
@Transactional(readOnly = true)
public class GetEpisodeService implements GetEpisodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetEpisodeService.class);

    private final EpisodeRepositoryPort repo;

    public GetEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca um episódio por seu identificador único.
     * 
     * @param id o identificador único do episódio
     * @return o episódio encontrado
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         episódio não existir
     */
    @Override
    @Observed(name = "episode.get", contextualName = "get-episode-service")
    @Measured("cinelog.service.episode.get")
    @AlertIfSlow(thresholdMs = 500)
    @Cacheable(value = "episodeById", key = "#id")
    public Episode execute(Long id) {
        log.debug("Buscando episódio. ID: {}", id);
        try {
            Episode episode = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Episode not found: " + id));
            log.debug("Episódio encontrado. ID: {}, Nome: {}", id, episode.getName());
            return episode;
        } catch (DomainException e) {
            log.warn("Episódio não encontrado. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar episódio. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}