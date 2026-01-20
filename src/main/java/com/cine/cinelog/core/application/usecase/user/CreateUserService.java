package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.CreateUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserEmailUniquenessPolicy;
import com.cine.cinelog.core.domain.policy.UserPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Serviço responsável por criar novos usuários no sistema.
 * 
 * <p>
 * Este caso de uso coordena a criação de um novo usuário, aplicando
 * validações rigorosas para garantir a integridade dos dados:
 * <ul>
 * <li>Validação de campos obrigatórios (nome, email, senha)</li>
 * <li>Validação de formato de email</li>
 * <li>Verificação de unicidade de email (não pode haver emails duplicados)</li>
 * <li>Aplicação de políticas de domínio do usuário</li>
 * </ul>
 * 
 * <p>
 * O serviço realiza logging detalhado de todas as etapas do processo
 * de criação para facilitar auditoria e troubleshooting.
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link CreateUserUseCase} e utilizando a porta de saída
 * {@link UserRepositoryPort}
 * para persistência dos dados.
 * 
 * @since 1.0
 * @see CreateUserUseCase
 * @see UserPolicy
 * @see UserEmailUniquenessPolicy
 * @see UserRepositoryPort
 */
@Transactional
public class CreateUserService implements CreateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateUserService.class);

    private final UserRepositoryPort userRepo;
    private final UserPolicy userPolicy;
    private final UserEmailUniquenessPolicy uniqueness;

    public CreateUserService(UserRepositoryPort userRepo, UserPolicy userPolicy, UserEmailUniquenessPolicy uniqueness) {
        this.userRepo = userRepo;
        this.userPolicy = userPolicy;
        this.uniqueness = uniqueness;
    }

    /**
     * Executa a criação de um novo usuário no sistema.
     * 
     * <p>
     * Aplica todas as validações necessárias antes de persistir,
     * incluindo validação de campos, formato de email e unicidade.
     * 
     * @param user o usuário a ser criado, contendo nome, email e senha
     * @return o usuário criado e persistido, com ID gerado
     * @throws DomainException se o usuário violar alguma regra de negócio ou
     *                         política de domínio
     * @throws DomainException se o email já estiver em uso por outro usuário
     */
    @Override
    @Observed(name = "user.create", contextualName = "create-user-service")
    @Measured("cinelog.service.user.create")
    @AuditableAction(module = "USER", action = "CREATE", description = "Criação de novo usuário no sistema")
    @SecureOperation(module = "USER")
    public User execute(User user) {
        log.debug("Iniciando execute. Parâmetros: {}", Map.of("name", user.getName(), "email", user.getEmail()));

        try {
            userPolicy.validateCreate(user);
            log.debug("Validação de política concluída para usuário: {}", user.getEmail());

            uniqueness.validateCreate(user);
            log.debug("Validação de unicidade de email concluída: {}", user.getEmail());

            User saved = userRepo.save(user);

            log.info("Usuário criado com sucesso. ID: {}, Email: {}", saved.getId(), saved.getEmail());
            log.debug("Finalizando execute. Resultado: {}", saved);

            return saved;
        } catch (Exception e) {
            log.error("Erro ao criar usuário. Parâmetros: {}. Erro: {}",
                    Map.of("name", user.getName(), "email", user.getEmail()), e.getMessage(), e);
            throw e;
        }
    }
}
