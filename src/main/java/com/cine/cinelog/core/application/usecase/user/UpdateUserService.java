package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.UpdateUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserEmailUniquenessPolicy;
import com.cine.cinelog.core.domain.policy.UserPolicy;
import com.cine.cinelog.core.domain.policy.UserUpdatePolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Serviço responsável por atualizar os dados de um usuário existente.
 * 
 * <p>
 * Este caso de uso coordena a atualização de um usuário, aplicando
 * múltiplas camadas de validação para garantir a integridade dos dados:
 * <ul>
 * <li>Busca o usuário existente</li>
 * <li>Aplica os dados de atualização (patch parcial)</li>
 * <li>Valida políticas de atualização (mudanças permitidas)</li>
 * <li>Valida políticas gerais do usuário</li>
 * <li>Verifica unicidade de email (se alterado)</li>
 * <li>Persiste as alterações</li>
 * </ul>
 * 
 * <p>
 * O serviço realiza logging detalhado de todas as etapas do processo
 * de atualização para facilitar auditoria e troubleshooting.
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link UpdateUserUseCase} e utilizando a porta de saída
 * {@link UserRepositoryPort}
 * para persistência dos dados.
 * 
 * @since 1.0
 * @see UpdateUserUseCase
 * @see UserPolicy
 * @see UserUpdatePolicy
 * @see UserEmailUniquenessPolicy
 * @see UserRepositoryPort
 */
@Transactional
public class UpdateUserService implements UpdateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserService.class);

    private final UserRepositoryPort repo;
    private final UserPolicy userPolicy;
    private final UserUpdatePolicy updatePolicy;
    private final UserEmailUniquenessPolicy uniqueness;

    public UpdateUserService(UserRepositoryPort repo, UserPolicy userPolicy, UserUpdatePolicy updatePolicy,
            UserEmailUniquenessPolicy uniqueness) {
        this.repo = repo;
        this.userPolicy = userPolicy;
        this.updatePolicy = updatePolicy;
        this.uniqueness = uniqueness;
    }

    /**
     * Executa a atualização de um usuário existente.
     * 
     * <p>
     * Aplica múltiplas validações antes de persistir, incluindo validação
     * de campos, políticas de atualização e unicidade de email (se alterado).
     * 
     * @param id    o identificador único do usuário a ser atualizado
     * @param patch os novos dados para atualização (campos nulos são ignorados)
     * @return o usuário atualizado e persistido
     * @throws DomainException com código {@link ErrorCode#USER_NOT_FOUND} se o
     *                         usuário não existir
     * @throws DomainException se os novos dados violarem alguma regra de negócio ou
     *                         política de domínio
     * @throws DomainException se o novo email já estiver em uso por outro usuário
     */
    @Override
    @Observed(name = "user.update", contextualName = "update-user-service")
    @Measured("cinelog.service.user.update")
    @AuditableAction(module = "USER", action = "UPDATE", description = "Atualização de dados do usuário")
    public User execute(Long id, User patch) {
        log.debug("Iniciando execute. Parâmetros: {}", Map.of("id", id, "patchName", patch.getName()));

        try {
            var current = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(ErrorCode.USER_NOT_FOUND));

            log.debug("Usuário encontrado para atualização. ID: {}", id);

            var updated = current.updateFrom(patch);

            updatePolicy.validate(current, (User) updated);
            log.debug("Validação de política de atualização concluída");

            userPolicy.validateUpdate((User) updated);
            log.debug("Validação de política de usuário concluída");

            uniqueness.validateUpdate(id, (User) updated);
            log.debug("Validação de unicidade concluída");

            User saved = repo.save((User) updated);

            log.info("Usuário atualizado com sucesso. ID: {}", id);
            log.debug("Finalizando execute. Resultado: {}", saved);

            return saved;
        } catch (Exception e) {
            log.error("Erro ao atualizar usuário. Parâmetros: {}. Erro: {}",
                    Map.of("id", id, "patchName", patch.getName()), e.getMessage(), e);
            throw e;
        }
    }
}
