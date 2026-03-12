package com.cine.cinelog.core.application.usecase.seasons;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.season.CreateSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.policy.SeasonPolicy;
import com.cine.cinelog.core.domain.policy.SeasonUniquenessPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por criar novas temporadas de séries.
 *
 * <p>
 * Uma temporada representa um conjunto de episódios de uma série,
 * tipicamente lançados em um mesmo período.
 *
 * <p>
 * Este serviço aplica validações rigorosas:
 * <ul>
 * <li>Valida campos obrigatórios e número da temporada</li>
 * <li>Verifica se a série (mídia) associada é do tipo SERIES</li>
 * <li>Garante unicidade: não permite temporadas duplicadas (mesmo número) para
 * uma série</li>
 * <li>Valida limite máximo de temporadas por série</li>
 * </ul>
 *
 * @since 1.0
 * @see CreateSeasonUseCase
 * @see SeasonRepositoryPort
 * @see SeasonPolicy
 * @see SeasonUniquenessPolicy
 */
@Transactional
public class CreateSeasonService implements CreateSeasonUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateSeasonService.class);

    private final SeasonRepositoryPort repo;
    private final SeasonPolicy policy;
    private final SeasonUniquenessPolicy uniquenessPolicy;

    public CreateSeasonService(SeasonRepositoryPort repo, SeasonPolicy policy,
            SeasonUniquenessPolicy uniquenessPolicy) {
        this.repo = repo;
        this.policy = policy;
        this.uniquenessPolicy = uniquenessPolicy;
    }

    /**
     * Executa a criação de uma nova temporada.
     *
     * @param season a temporada a ser criada, associada a uma série
     * @return a temporada criada e persistida, com ID gerado
     * @throws DomainException se a série não for do tipo SERIES ou se violar regras
     *                         de unicidade/limite
     */
    @Override
    @Observed(name = "season.create", contextualName = "create-season-service")
    @Measured("cinelog.service.season.create")
    @AuditableAction(module = "SEASON", action = "CREATE", description = "Criação de nova temporada de série")
    @CacheEvict(value = "seasonsPage", allEntries = true)
    public Season execute(Season season) {
        log.debug("Iniciando criação de temporada. Parâmetros: {}",
                Map.of("seasonNumber", season.getSeasonNumber(),
                        "mediaId", season.getMediaId(),
                        "name", season.getName() != null ? season.getName() : "null"));

        try {
            log.debug("Validando políticas de criação de temporada");
            policy.validateCreate(season); // S1, S2, S4

            log.debug("Validando unicidade de temporada");
            uniquenessPolicy.validateCreate(season); // S3

            Season saved = repo.save(season);
            log.info("Temporada criada com sucesso. ID: {}, Número: {}", saved.getId(), saved.getSeasonNumber());
            return saved;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Erro de validação ao criar temporada: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao criar temporada. Número: {}, Erro: {}", season.getSeasonNumber(),
                    e.getMessage(), e);
            throw e;
        }
    }
}
