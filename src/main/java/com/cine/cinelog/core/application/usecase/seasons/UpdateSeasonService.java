package com.cine.cinelog.core.application.usecase.seasons;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.season.UpdateSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por atualizar dados de temporadas existentes.
 * 
 * <p>
 * Permite alterar informações como número da temporada, nome e série associada.
 * 
 * @since 1.0
 * @see UpdateSeasonUseCase
 * @see SeasonRepositoryPort
 */
@Transactional
public class UpdateSeasonService implements UpdateSeasonUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateSeasonService.class);

    private final SeasonRepositoryPort repo;

    public UpdateSeasonService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a atualização de uma temporada existente.
     * 
     * @param id     o identificador único da temporada a ser atualizada
     * @param season os novos dados da temporada
     * @return a temporada atualizada e persistida
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se a
     *                         temporada não existir
     */
    @Override
    @Observed(name = "season.update", contextualName = "update-season-service")
    @Measured("cinelog.service.season.update")
    @AuditableAction(module = "SEASON", action = "UPDATE", description = "Atualização de temporada")
    public Season execute(Long id, Season season) {
        log.debug("Iniciando atualização de temporada. ID: {}", id);

        try {
            log.debug("Buscando temporada existente. ID: {}", id);
            Season existing = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Season not found: " + id));

            log.debug("Temporada encontrada. Aplicando atualizações");
            existing.setSeasonNumber(season.getSeasonNumber());
            existing.setName(season.getName());
            existing.setMediaId(season.getMediaId());

            Season saved = repo.save(existing);
            log.info("Temporada atualizada com sucesso. ID: {}, Número: {}", id, saved.getSeasonNumber());
            return saved;
        } catch (DomainException e) {
            log.warn("Temporada não encontrada. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar temporada. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}