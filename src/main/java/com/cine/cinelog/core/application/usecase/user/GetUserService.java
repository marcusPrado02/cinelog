package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.GetUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar um usuário específico por seu identificador.
 * 
 * <p>
 * Este caso de uso recupera os dados completos de um usuário cadastrado
 * no sistema, utilizando cache para otimizar performance em consultas
 * repetidas.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Resultado cacheado com chave baseada no ID</li>
 * <li>Lança exceção de domínio caso o usuário não seja encontrado</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link GetUserUseCase} e utilizando a porta de saída
 * {@link UserRepositoryPort}
 * para consulta dos dados.
 * 
 * @since 1.0
 * @see GetUserUseCase
 * @see UserRepositoryPort
 */
@Transactional(readOnly = true)
public class GetUserService implements GetUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetUserService.class);

    private final UserRepositoryPort repo;

    public GetUserService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca um usuário por seu identificador único.
     * 
     * <p>
     * O resultado é armazenado em cache para melhorar a performance
     * em consultas subsequentes do mesmo usuário.
     * 
     * @param id o identificador único do usuário a ser buscado
     * @return o usuário encontrado com todos os seus dados
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         usuário não existir
     */
    @Cacheable(value = "userById", key = "#id")
    @Observed(name = "user.get", contextualName = "get-user-service")
    @Measured("cinelog.service.user.get")
    @AlertIfSlow(thresholdMs = 500)
    @Override
    public User execute(Long id) {
        log.debug("Iniciando busca de usuário no service. ID: {}", id);

        try {
            User user = repo.findById(id).orElseThrow(() -> {
                log.warn("Usuário não encontrado. ID: {}", id);
                return DomainException.of(ErrorCode.GEN_NOT_FOUND, "User not found: " + id);
            });

            log.debug("Usuário encontrado. ID: {}, Email: {}", user.getId(), user.getEmail());
            return user;

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar usuário. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}