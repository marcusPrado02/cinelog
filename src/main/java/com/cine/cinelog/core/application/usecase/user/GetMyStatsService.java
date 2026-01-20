package com.cine.cinelog.core.application.usecase.user;

import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.core.application.ports.in.user.GetMyStatsUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.UserStats;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import io.micrometer.observation.annotation.Observed;

/**
 * Serviço responsável por obter estatísticas de visualização do usuário
 * autenticado.
 * 
 * <p>
 * Este caso de uso calcula e retorna estatísticas agregadas sobre o histórico
 * de visualizações do usuário atualmente autenticado no sistema:
 * <ul>
 * <li>Total de mídias assistidas</li>
 * <li>Total de mídias avaliadas</li>
 * <li>Avaliação média dada pelo usuário</li>
 * <li>Data da primeira visualização</li>
 * <li>Data da última visualização</li>
 * </ul>
 * 
 * <p>
 * As estatísticas são calculadas dinamicamente a partir dos registros
 * de visualização (watch entries) do usuário, aplicando regras de negócio
 * para cálculo de médias e agregações.
 * 
 * <p>
 * O serviço utiliza o {@link CurrentUserProvider} para identificar o usuário
 * autenticado automaticamente, sem necessidade de passar o ID explicitamente.
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link GetMyStatsUseCase} e utilizando a porta de saída
 * {@link WatchEntryRepositoryPort}
 * para consulta dos dados de visualização.
 * 
 * @since 1.0
 * @see GetMyStatsUseCase
 * @see UserStats
 * @see CurrentUserProvider
 * @see WatchEntryRepositoryPort
 */
@Transactional(readOnly = true)
public class GetMyStatsService implements GetMyStatsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetMyStatsService.class);

    private final WatchEntryRepositoryPort watchEntryRepo;
    private final CurrentUserProvider currentUserProvider;

    public GetMyStatsService(WatchEntryRepositoryPort watchEntryRepo,
            CurrentUserProvider currentUserProvider) {
        this.watchEntryRepo = watchEntryRepo;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Calcula e retorna as estatísticas de visualização do usuário autenticado.
     * 
     * <p>
     * Agrega dados de todos os registros de visualização do usuário,
     * calculando totais, médias e identificando datas de primeira e última
     * visualização.
     * 
     * @return as estatísticas completas do usuário com totais e médias calculadas
     * @throws DomainException se não houver usuário autenticado no contexto
     */
    @Observed(name = "user.mystats", contextualName = "get-mystats-service")
    @Measured("cinelog.service.user.mystats")
    @AlertIfSlow(thresholdMs = 1000)
    @Override
    public UserStats execute() {
        log.debug("Iniciando execute");

        try {
            var user = currentUserProvider.getRequiredCurrentUser();
            log.debug("Usuário atual identificado. ID: {}", user.id());

            long totalEntries = watchEntryRepo.countEntriesByUserId(user.id());
            long totalRated = watchEntryRepo.countRatedEntriesByUserId(user.id());
            var avgOpt = watchEntryRepo.averageRatingByUserId(user.id());

            Double rawAverage = avgOpt.orElse(null);
            LocalDate firstWatchDate = watchEntryRepo.findFirstWatchDateByUserId(user.id());
            LocalDate lastWatchDate = watchEntryRepo.findLastWatchDateByUserId(user.id());

            // Aqui ST1 + ST2 são aplicadas dentro do VO
            UserStats stats = UserStats.of(user.id(), totalEntries, totalRated, rawAverage, firstWatchDate,
                    lastWatchDate);

            log.info("Estatísticas calculadas para usuário {}. Total entries: {}, Total rated: {}",
                    user.id(), totalEntries, totalRated);
            log.debug("Finalizando execute. Resultado: totalEntries={}, avgRating={}", totalEntries, rawAverage);

            return stats;
        } catch (Exception e) {
            log.error("Erro ao obter estatísticas do usuário. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
