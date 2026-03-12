package com.cine.cinelog.core.application.usecase.media;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaPolicy;
import com.cine.cinelog.core.domain.validator.MediaValidatorFactory;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Serviço responsável por criar novas mídias (filmes ou séries) no sistema.
 *
 * <p>
 * Este caso de uso coordena a criação de uma nova mídia, aplicando as seguintes
 * operações:
 * <ul>
 * <li>Normalização dos dados da mídia (título, descrição, etc.)</li>
 * <li>Validação das invariantes de domínio através de {@link MediaPolicy}</li>
 * <li>Validação específica por tipo através de
 * {@link MediaValidatorFactory}</li>
 * <li>Persistência da mídia no repositório</li>
 * </ul>
 *
 * <p>
 * As políticas aplicadas incluem:
 * <ul>
 * <li>Validação do tipo de mídia (filme ou série)</li>
 * <li>Validação de campos obrigatórios</li>
 * <li>Validação de regras específicas por tipo (Template Method Pattern)</li>
 * </ul>
 *
 * <p>
 * <strong>Design Patterns Aplicados</strong>:
 * <ul>
 * <li><strong>Template Method</strong>: Validadores específicos por tipo
 * (MovieValidator, SeriesValidator)</li>
 * <li><strong>Factory Method</strong>: MediaValidatorFactory seleciona
 * validador apropriado</li>
 * <li><strong>Strategy</strong>: MediaPolicy encapsula regras gerais de
 * validação</li>
 * </ul>
 *
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link CreateMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}
 * para persistência dos dados.
 *
 * @since 1.0
 * @see CreateMediaUseCase
 * @see MediaPolicy
 * @see MediaValidatorFactory
 * @see MediaRepositoryPort
 */
@Transactional
public class CreateMediaService implements CreateMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateMediaService.class);

    private final MediaRepositoryPort repo;
    private final MediaPolicy policy;

    public CreateMediaService(MediaRepositoryPort repo, MediaPolicy policy) {
        this.repo = repo;
        this.policy = policy;
    }

    /**
     * Executa a criação de uma nova mídia no sistema.
     *
     * @param media a mídia a ser criada, contendo todos os dados necessários
     * @return a mídia criada e persistida, com ID gerado
     * @throws DomainException se a mídia violar alguma regra de negócio ou política
     *                         de domínio
     */
    @Override
    @Observed(name = "media.create", contextualName = "create-media-service")
    @Measured("cinelog.service.media.create")
    @AuditableAction(module = "MEDIA", action = "CREATE", description = "Criação de nova mídia no catálogo")
    @CacheEvict(value = "mediaPage", allEntries = true)
    public Media execute(Media media) {
        log.debug("Iniciando criação de mídia no service. Dados: {}",
                Map.of("title", media.getTitle(), "type", media.getType(),
                        "releaseYear", media.getReleaseYear() != null ? media.getReleaseYear() : "N/A"));

        try {
            // Normalização dos dados
            log.debug("Normalizando dados da mídia. Título: {}", media.getTitle());
            media.normalize();

            // Validação de políticas de domínio (regras gerais)
            log.debug("Validando invariantes de domínio. Mídia: {}", media.getTitle());
            policy.validateInvariants(media);

            // Validação específica por tipo (Template Method Pattern)
            log.debug("Validando regras específicas do tipo {}. Mídia: {}",
                    media.getType(), media.getTitle());
            MediaValidatorFactory.validate(media);

            // Persistindo no banco de dados
            log.debug("Persistindo mídia no banco de dados. Título: {}", media.getTitle());
            Media saved = repo.save(media);

            log.info("Mídia criada com sucesso no service. ID: {}, Título: {}, Tipo: {}",
                    saved.getId(), saved.getTitle(), saved.getType());

            return saved;

        } catch (IllegalArgumentException e) {
            // Erros de validação (não precisa stacktrace completo)
            log.warn("Erro de validação ao criar mídia. Título: {}, Tipo: {}, Erro: {}",
                    media.getTitle(), media.getType(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // Erros inesperados (precisa stacktrace para debug)
            log.error("Erro inesperado ao criar mídia. Título: {}, Tipo: {}, Erro: {}",
                    media.getTitle(), media.getType(), e.getMessage(), e);
            throw e;
        }
    }
}
