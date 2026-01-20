package com.cine.cinelog.core.application.usecase.episodes;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.episodes.DeleteEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por excluir episódios do sistema.
 * 
 * <p>
 * Remove um episódio de uma temporada. A exclusão pode falhar se houver
 * dependências como registros de visualização associados ao episódio.
 * 
 * @since 1.0
 * @see DeleteEpisodeUseCase
 * @see EpisodeRepositoryPort
 */
@Transactional
public class DeleteEpisodeService implements DeleteEpisodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteEpisodeService.class);

    private final EpisodeRepositoryPort repo;

    public DeleteEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a exclusão de um episódio.
     * 
     * @param id o identificador único do episódio a ser excluído
     */
    @Override
    @Observed(name = "episode.delete", contextualName = "delete-episode-service")
    @Measured("cinelog.service.episode.delete")
    @AuditableAction(module = "EPISODE", action = "DELETE", description = "Exclusão de episódio")
    @SecureOperation(module = "EPISODE", value = "CONTENT_ADMIN")
    public void execute(Long id) {
        log.debug("Iniciando exclusão de episódio. ID: {}", id);
        try {
            repo.deleteById(id);
            log.info("Episódio excluído com sucesso. ID: {}", id);
        } catch (Exception e) {
            log.error("Erro ao excluir episódio. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}