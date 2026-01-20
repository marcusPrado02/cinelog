package com.cine.cinelog.core.application.usecase.media;

import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Serviço responsável por atualizar os dados de uma mídia existente.
 * 
 * <p>
 * Este caso de uso coordena a atualização de uma mídia, realizando as seguintes
 * operações:
 * <ul>
 * <li>Busca a mídia existente pelo ID</li>
 * <li>Aplica os dados de atualização mantendo campos não fornecidos</li>
 * <li>Valida as invariantes de domínio após a atualização</li>
 * <li>Persiste as alterações no repositório</li>
 * </ul>
 * 
 * <p>
 * As políticas de validação incluem:
 * <ul>
 * <li>Verificação de mudança de tipo de mídia (filme ↔ série)</li>
 * <li>Validação de campos obrigatórios</li>
 * <li>Validação de regras específicas por tipo</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link UpdateMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}
 * para persistência dos dados.
 * 
 * @since 1.0
 * @see UpdateMediaUseCase
 * @see MediaPolicy
 * @see MediaRepositoryPort
 */
@Transactional
public class UpdateMediaService implements UpdateMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateMediaService.class);

    private final MediaRepositoryPort repo;
    private final MediaPolicy mediaPolicy;

    public UpdateMediaService(MediaRepositoryPort repo, MediaPolicy mediaPolicy) {
        this.repo = repo;
        this.mediaPolicy = mediaPolicy;
    }

    /**
     * Executa a atualização de uma mídia existente.
     * 
     * @param id   o identificador único da mídia a ser atualizada
     * @param data os novos dados para atualização (campos nulos são ignorados)
     * @return a mídia atualizada e persistida
     * @throws DomainException com código {@link ErrorCode#MEDIA_NOT_FOUND} se a
     *                         mídia não existir
     * @throws DomainException se os novos dados violarem alguma regra de negócio ou
     *                         política de domínio
     */
    @Override
    @Observed(name = "media.update", contextualName = "update-media-service")
    @Measured("cinelog.service.media.update")
    @AuditableAction(module = "MEDIA", action = "UPDATE", description = "Atualização de dados de mídia")
    public Media execute(Long id, Media data) {
        log.debug("Iniciando atualização de mídia no service. ID: {}", id);

        try {
            // Busca mídia existente
            log.debug("Buscando mídia existente. ID: {}", id);
            var current = repo.findById(id).orElseThrow(() -> {
                log.warn("Mídia não encontrada para atualização. ID: {}", id);
                return DomainException.of(ErrorCode.MEDIA_NOT_FOUND, "Media not found: " + id);
            });

            // Aplica atualização
            log.debug("Aplicando dados de atualização. Mídia: {}", current.getTitle());
            var updated = current.updateFrom(data);

            // Valida invariantes
            log.debug("Validando invariantes após atualização. Mídia: {}", updated.getTitle());
            mediaPolicy.validateInvariants(updated);

            // Persiste
            Media saved = repo.save(updated);
            log.info("Mídia atualizada com sucesso. ID: {}, Título: {}", saved.getId(), saved.getTitle());

            return saved;

        } catch (DomainException e) {
            log.warn("Erro de domínio ao atualizar mídia. ID: {}, Erro: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar mídia. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
