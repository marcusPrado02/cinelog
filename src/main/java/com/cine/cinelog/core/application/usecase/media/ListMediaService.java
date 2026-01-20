package com.cine.cinelog.core.application.usecase.media;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.List;

/**
 * Serviço responsável por listar mídias do sistema com paginação.
 * 
 * <p>
 * Este caso de uso retorna uma lista paginada de todas as mídias
 * (filmes e séries) cadastradas no sistema, permitindo navegação eficiente
 * através de grandes volumes de dados.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Suporte a paginação através de {@link PageQuery}</li>
 * <li>Resultado cacheado para melhorar performance</li>
 * <li>Retorna metadata de paginação (total, páginas, etc.)</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link ListMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}
 * para consulta dos dados.
 * 
 * @since 1.0
 * @see ListMediaUseCase
 * @see MediaRepositoryPort
 * @see PageQuery
 * @see PageResult
 */
@Transactional(readOnly = true)
public class ListMediaService implements ListMediaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListMediaService.class);

    private final MediaRepositoryPort repo;

    public ListMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todas as mídias do sistema de forma paginada.
     * 
     * <p>
     * O resultado é cacheado baseado nos parâmetros da consulta
     * (página, tamanho, ordenação) para otimizar performance.
     * 
     * @param pageQuery os parâmetros de paginação (página, tamanho, ordenação)
     * @return resultado paginado contendo as mídias e metadados de paginação
     */
    @Observed(name = "media.list", contextualName = "list-media-service")
    @Measured("cinelog.service.media.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "mediaPage", key = "#pageQuery.toString()")
    @Override
    public PageResult<Media> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de mídias. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));

        try {
            // Simples delegação para porta de persistência. A paginação e o
            // filtro são repassados conforme recebido pela camada de aplicação.
            PageResult<Media> result = repo.listAll(pageQuery);

            log.debug("Listagem de mídias concluída. Total encontrado: {}", result.totalElements());
            return result;

        } catch (Exception e) {
            log.error("Erro inesperado ao listar mídias. Parâmetros: {}, Erro: {}",
                    Map.of("page", pageQuery.page(), "size", pageQuery.size()), e.getMessage(), e);
            throw e;
        }
    }
}