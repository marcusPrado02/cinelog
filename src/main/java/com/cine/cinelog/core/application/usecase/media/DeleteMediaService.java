package com.cine.cinelog.core.application.usecase.media;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaDeletionPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Serviço responsável por excluir uma mídia do sistema.
 *
 * <p>
 * Este caso de uso coordena a exclusão de uma mídia, aplicando validações
 * de integridade referencial antes de permitir a remoção:
 * <ul>
 * <li>Busca a mídia existente pelo ID</li>
 * <li>Valida se a mídia pode ser excluída (verifica dependências)</li>
 * <li>Remove a mídia do repositório se todas as validações passarem</li>
 * </ul>
 *
 * <p>
 * As políticas de deleção verificam:
 * <ul>
 * <li>Se existem temporadas associadas (para séries)</li>
 * <li>Se existem créditos (pessoas) associados à mídia</li>
 * <li>Se existem registros de visualização (watch entries) associados</li>
 * <li>Se a mídia está em watchlists de usuários</li>
 * </ul>
 *
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link DeleteMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}
 * para persistência dos dados.
 *
 * @since 1.0
 * @see DeleteMediaUseCase
 * @see MediaDeletionPolicy
 * @see MediaRepositoryPort
 */
@Transactional
public class DeleteMediaService implements DeleteMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMediaService.class);

    private final MediaRepositoryPort repo;
    private final MediaDeletionPolicy deletionPolicy;

    public DeleteMediaService(MediaRepositoryPort repo,
            MediaDeletionPolicy deletionPolicy) {
        this.repo = repo;
        this.deletionPolicy = deletionPolicy;
    }

    /**
     * Executa a exclusão de uma mídia do sistema.
     *
     * <p>
     * A exclusão é bloqueada se houver dependências (temporadas, créditos,
     * visualizações ou watchlists), garantindo a integridade referencial dos dados.
     *
     * @param id o identificador único da mídia a ser excluída
     * @throws DomainException com código {@link ErrorCode#MEDIA_NOT_FOUND} se a
     *                         mídia não existir
     * @throws DomainException se a mídia não puder ser excluída devido a
     *                         dependências existentes
     */
    @Override
    @Observed(name = "media.delete", contextualName = "delete-media-service")
    @Measured("cinelog.service.media.delete")
    @AuditableAction(module = "MEDIA", action = "DELETE", description = "Exclusão de mídia do catálogo")
    @SecureOperation(module = "MEDIA", value = "MEDIA_ADMIN")
    @Caching(evict = {
            @CacheEvict(value = "mediaPage", allEntries = true),
            @CacheEvict(value = "mediaById", key = "#id")
    })
    public void execute(Long id) {
        log.debug("Iniciando exclusão de mídia no service. ID: {}", id);

        try {
            // Busca mídia existente
            log.debug("Buscando mídia para exclusão. ID: {}", id);
            Media media = repo.findById(id).orElseThrow(() -> {
                log.warn("Mídia não encontrada para exclusão. ID: {}", id);
                return DomainException.of(ErrorCode.MEDIA_NOT_FOUND, "Media not found: " + id);
            });

            // MD1–MD5: valida se pode deletar
            log.debug("Validando política de exclusão. Mídia: {}", media.getTitle());
            deletionPolicy.validateDelete(media);

            // Se passou pela policy, pode deletar de verdade
            log.debug("Removendo mídia do banco de dados. ID: {}, Título: {}", id, media.getTitle());
            repo.deleteById(id);

            log.info("Mídia excluída com sucesso. ID: {}, Título: {}", id, media.getTitle());

        } catch (DomainException e) {
            log.warn("Erro de domínio ao excluir mídia. ID: {}, Erro: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir mídia. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
