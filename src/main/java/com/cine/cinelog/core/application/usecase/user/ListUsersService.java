package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.user.ListUsersUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço responsável por listar usuários do sistema com paginação.
 * 
 * <p>
 * Este caso de uso retorna uma lista paginada de todos os usuários
 * cadastrados no sistema, permitindo navegação eficiente através de
 * grandes volumes de dados.
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
 * {@link ListUsersUseCase} e utilizando a porta de saída
 * {@link UserRepositoryPort}
 * para consulta dos dados.
 * 
 * @since 1.0
 * @see ListUsersUseCase
 * @see UserRepositoryPort
 * @see PageQuery
 * @see PageResult
 */
@Transactional(readOnly = true)
public class ListUsersService implements ListUsersUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListUsersService.class);

    private final UserRepositoryPort repo;

    public ListUsersService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todos os usuários do sistema de forma paginada.
     * 
     * <p>
     * O resultado é cacheado baseado nos parâmetros da consulta
     * (página, tamanho, ordenação) para otimizar performance.
     * 
     * @param pageQuery os parâmetros de paginação (página, tamanho, ordenação)
     * @return resultado paginado contendo os usuários e metadados de paginação
     */
    @Observed(name = "user.list", contextualName = "list-users-service")
    @Measured("cinelog.service.user.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "usersPage", key = "#pageQuery.toString()")
    @Override
    public PageResult<User> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de usuários. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));

        try {
            PageResult<User> result = repo.findAll(pageQuery);
            log.debug("Listagem de usuários concluída. Total encontrado: {}", result.totalElements());
            return result;

        } catch (Exception e) {
            log.error("Erro inesperado ao listar usuários. Parâmetros: {}, Erro: {}",
                    Map.of("page", pageQuery.page(), "size", pageQuery.size()), e.getMessage(), e);
            throw e;
        }
    }
}