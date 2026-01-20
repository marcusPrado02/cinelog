package com.cine.cinelog.core.application.usecase.media;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Serviço responsável por buscar uma mídia específica por seu identificador.
 * 
 * <p>
 * Este caso de uso recupera uma mídia (filme ou série) pelo ID fornecido,
 * utilizando cache para otimizar performance em consultas repetidas.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Resultado cacheado com chave baseada no ID</li>
 * <li>Lança exceção de domínio caso a mídia não seja encontrada</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link GetMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}
 * para consulta dos dados.
 * 
 * @since 1.0
 * @see GetMediaUseCase
 * @see MediaRepositoryPort
 */
@Transactional(readOnly = true)
public class GetMediaService implements GetMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetMediaService.class);

    private final MediaRepositoryPort repo;

    public GetMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca uma mídia por seu identificador único.
     * 
     * <p>
     * O resultado é armazenado em cache para melhorar a performance
     * em consultas subsequentes da mesma mídia.
     * 
     * @param id o identificador único da mídia a ser buscada
     * @return a mídia encontrada com todos os seus dados
     * @throws DomainException com código {@link ErrorCode#MEDIA_NOT_FOUND} se a
     *                         mídia não existir
     */
    @Cacheable(value = "mediaById", key = "#id")
    @Observed(name = "media.get", contextualName = "get-media-service")
    @Measured("cinelog.service.media.get")
    @AlertIfSlow(thresholdMs = 500)
    @Override
    public Media execute(Long id) {
        log.debug("Iniciando busca de mídia no service. ID: {}", id);

        try {
            // Recupera a mídia pelo id ou lança exceção clara
            Media media = repo.findById(id).orElseThrow(() -> {
                log.warn("Mídia não encontrada. ID: {}", id);
                return DomainException.of(ErrorCode.MEDIA_NOT_FOUND, "Media not found: " + id);
            });

            log.debug("Mídia encontrada. ID: {}, Título: {}", media.getId(), media.getTitle());
            return media;

        } catch (DomainException e) {
            // Já foi logado acima, apenas repropaga
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar mídia. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}