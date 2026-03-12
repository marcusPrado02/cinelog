package com.cine.cinelog.core.application.usecase.credits;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.credits.CreateCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por criar créditos (participações) de pessoas em mídias.
 *
 * <p>
 * Um crédito representa a participação de uma pessoa (ator, diretor, etc.)
 * em uma mídia (filme ou série), definindo seu papel/função nessa produção.
 *
 * <p>
 * Este serviço permite associar pessoas a mídias com funções específicas como:
 * <ul>
 * <li>Ator/Atriz (ACTOR)</li>
 * <li>Diretor (DIRECTOR)</li>
 * <li>Roteirista (WRITER)</li>
 * <li>Produtor (PRODUCER)</li>
 * <li>Outras funções definidas no enum
 * {@link com.cine.cinelog.core.domain.enums.Role}</li>
 * </ul>
 *
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link CreateCreditUseCase} e utilizando a porta de saída
 * {@link CreditRepositoryPort}.
 *
 * @since 1.0
 * @see CreateCreditUseCase
 * @see CreditRepositoryPort
 * @see Credit
 */
@Transactional
public class CreateCreditService implements CreateCreditUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateCreditService.class);

    private final CreditRepositoryPort repo;

    public CreateCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a criação de um novo crédito, associando uma pessoa a uma mídia.
     *
     * @param credit o crédito a ser criado, contendo pessoa, mídia e função
     * @return o crédito criado e persistido, com ID gerado
     */
    @Override
    @Observed(name = "credit.create", contextualName = "create-credit-service")
    @Measured("cinelog.service.credit.create")
    @AuditableAction(module = "CREDIT", action = "CREATE", description = "Criação de crédito (pessoa em mídia)")
    @CacheEvict(value = "creditsPage", allEntries = true)
    public Credit execute(Credit credit) {
        log.debug("Iniciando criação de crédito. Parâmetros: {}",
                Map.of("personId", credit.getPersonId(),
                        "mediaId", credit.getMediaId(),
                        "role", credit.getRole()));

        try {
            Credit saved = repo.save(credit);
            log.info("Crédito criado com sucesso. ID: {}, Função: {}", saved.getId(), saved.getRole());
            return saved;
        } catch (Exception e) {
            log.error("Erro ao criar crédito. PersonId: {}, MediaId: {}, Erro: {}",
                    credit.getPersonId(), credit.getMediaId(), e.getMessage(), e);
            throw e;
        }
    }
}
