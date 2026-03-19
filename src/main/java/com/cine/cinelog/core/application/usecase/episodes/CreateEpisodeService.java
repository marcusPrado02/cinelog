package com.cine.cinelog.core.application.usecase.episodes;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.episodes.CreateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por criar novos episódios de temporadas de séries.
 *
 * <p>
 * Um episódio representa um capítulo individual de uma temporada de série,
 * contendo informações como número do episódio, nome, data de exibição, etc.
 *
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link CreateEpisodeUseCase} e utilizando a porta de saída
 * {@link EpisodeRepositoryPort}.
 *
 * @since 1.0
 * @see CreateEpisodeUseCase
 * @see EpisodeRepositoryPort
 * @see Episode
 */
@Transactional
public class CreateEpisodeService implements CreateEpisodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateEpisodeService.class);

    private final EpisodeRepositoryPort repo;

    public CreateEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a criação de um novo episódio.
     *
     * @param episode o episódio a ser criado, associado a uma temporada
     * @return o episódio criado e persistido, com ID gerado
     */
    @Override
    @Observed(name = "episode.create", contextualName = "create-episode-service")
    @Measured("cinelog.service.episode.create")
    @AuditableAction(module = "EPISODE", action = "CREATE", description = "Criação de novo episódio")
    @CacheEvict(value = "episodesPage", allEntries = true)
    public Episode execute(Episode episode) {
        log.debug("Iniciando criação de episódio. Parâmetros: {}",
                Map.of("name", String.valueOf(episode.getName()),
                        "episodeNumber", String.valueOf(episode.getEpisodeNumber()),
                        "seasonId", String.valueOf(episode.getSeasonId())));

        try {
            Episode saved = repo.save(episode);
            log.info("Episódio criado com sucesso. ID: {}, Nome: {}", saved.getId(), saved.getName());
            return saved;
        } catch (Exception e) {
            log.error("Erro ao criar episódio. Nome: {}, Erro: {}", episode.getName(), e.getMessage(), e);
            throw e;
        }
    }
}
