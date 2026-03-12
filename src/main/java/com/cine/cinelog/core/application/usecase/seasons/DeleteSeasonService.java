package com.cine.cinelog.core.application.usecase.seasons;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.season.DeleteSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.policy.SeasonDeletionPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por excluir temporadas do sistema.
 *
 * <p>
 * Valida se a temporada pode ser excluída antes de removê-la.
 * A exclusão é bloqueada se houver episódios associados à temporada.
 *
 * @since 1.0
 * @see DeleteSeasonUseCase
 * @see SeasonRepositoryPort
 * @see SeasonDeletionPolicy
 */
@Transactional
public class DeleteSeasonService implements DeleteSeasonUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteSeasonService.class);

    private final SeasonRepositoryPort repo;
    private final SeasonDeletionPolicy deletionPolicy;

    public DeleteSeasonService(SeasonRepositoryPort repo, SeasonDeletionPolicy deletionPolicy) {
        this.repo = repo;
        this.deletionPolicy = deletionPolicy;
    }

    /**
     * Executa a exclusão de uma temporada.
     *
     * @param id o identificador único da temporada a ser excluída
     * @throws DomainException com código {@link ErrorCode#SEASON_NOT_FOUND} se a
     *                         temporada não existir
     * @throws DomainException se houver episódios associados à temporada
     */
    @Override
    @Observed(name = "season.delete", contextualName = "delete-season-service")
    @Measured("cinelog.service.season.delete")
    @AuditableAction(module = "SEASON", action = "DELETE", description = "Exclusão de temporada")
    @SecureOperation(module = "SEASON", value = "CONTENT_ADMIN")
    @Caching(evict = {
            @CacheEvict(value = "seasonsPage", allEntries = true),
            @CacheEvict(value = "seasonById", key = "#id")
    })
    public void execute(Long id) {
        log.debug("Iniciando exclusão de temporada. ID: {}", id);
        try {
            log.debug("Buscando temporada para exclusão. ID: {}", id);
            Season season = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(ErrorCode.SEASON_NOT_FOUND, id));

            log.debug("Validando política de exclusão de temporada");
            deletionPolicy.validateDelete(season);

            repo.deleteById(id);
            log.info("Temporada excluída com sucesso. ID: {}", id);
        } catch (DomainException e) {
            log.warn("Erro ao excluir temporada. ID: {}, Erro: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir temporada. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
