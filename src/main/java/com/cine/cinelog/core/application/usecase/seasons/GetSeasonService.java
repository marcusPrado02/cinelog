package com.cine.cinelog.core.application.usecase.seasons;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.season.GetSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar uma temporada específica por seu
 * identificador.
 * 
 * <p>
 * Recupera os dados completos de uma temporada, incluindo número e série
 * associada.
 * 
 * @since 1.0
 * @see GetSeasonUseCase
 * @see SeasonRepositoryPort
 */
@Transactional(readOnly = true)
public class GetSeasonService implements GetSeasonUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetSeasonService.class);

    private final SeasonRepositoryPort repo;

    public GetSeasonService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca uma temporada por seu identificador único.
     * 
     * @param id o identificador único da temporada
     * @return a temporada encontrada
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se a
     *                         temporada não existir
     */
    @Override
    @Observed(name = "season.get", contextualName = "get-season-service")
    @Measured("cinelog.service.season.get")
    @AlertIfSlow(thresholdMs = 500)
    @Cacheable(value = "seasonById", key = "#id")
    public Season execute(Long id) {
        log.debug("Buscando temporada. ID: {}", id);
        try {
            Season season = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Season not found: " + id));
            log.debug("Temporada encontrada. ID: {}, Número: {}", id, season.getSeasonNumber());
            return season;
        } catch (DomainException e) {
            log.warn("Temporada não encontrada. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar temporada. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}