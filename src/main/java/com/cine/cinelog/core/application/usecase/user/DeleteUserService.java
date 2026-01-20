package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.DeleteUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.policy.UserDeletionPolicy;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Serviço responsável por excluir um usuário do sistema.
 * 
 * <p>
 * Este caso de uso coordena a exclusão de um usuário, aplicando validações
 * de integridade referencial antes de permitir a remoção:
 * <ul>
 * <li>Busca o usuário existente pelo ID</li>
 * <li>Valida se o usuário pode ser excluído (verifica dependências)</li>
 * <li>Remove o usuário do repositório se todas as validações passarem</li>
 * </ul>
 * 
 * <p>
 * As políticas de deleção verificam:
 * <ul>
 * <li>Se existem registros de visualização (watch entries) do usuário</li>
 * <li>Se existem itens na watchlist do usuário</li>
 * <li>Outras dependências que impedem a exclusão</li>
 * </ul>
 * 
 * <p>
 * O serviço realiza logging detalhado do processo de exclusão
 * para facilitar auditoria e troubleshooting.
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link DeleteUserUseCase} e utilizando a porta de saída
 * {@link UserRepositoryPort}
 * para persistência dos dados.
 * 
 * @since 1.0
 * @see DeleteUserUseCase
 * @see UserDeletionPolicy
 * @see UserRepositoryPort
 */
@Transactional
public class DeleteUserService implements DeleteUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserService.class);

    private final UserRepositoryPort repo;
    private final UserDeletionPolicy deletionPolicy;

    public DeleteUserService(UserRepositoryPort repo, UserDeletionPolicy deletionPolicy) {
        this.repo = repo;
        this.deletionPolicy = deletionPolicy;
    }

    /**
     * Executa a exclusão de um usuário do sistema.
     * 
     * <p>
     * A exclusão é bloqueada se houver dependências (visualizações, watchlist),
     * garantindo a integridade referencial dos dados.
     * 
     * @param id o identificador único do usuário a ser excluído
     * @throws DomainException com código {@link ErrorCode#USER_NOT_FOUND} se o
     *                         usuário não existir
     * @throws DomainException se o usuário não puder ser excluído devido a
     *                         dependências existentes
     */
    @Override
    @Observed(name = "user.delete", contextualName = "delete-user-service")
    @Measured("cinelog.service.user.delete")
    @AuditableAction(module = "USER", action = "DELETE", description = "Exclusão de usuário do sistema")
    @SecureOperation(module = "USER", value = "USER_ADMIN")
    public void execute(Long id) {
        log.debug("Iniciando execute. Parâmetros: {}", Map.of("id", id));

        try {
            var user = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(ErrorCode.USER_NOT_FOUND));

            log.debug("Usuário encontrado para exclusão. ID: {}", id);

            deletionPolicy.validateDelete(user);
            log.debug("Validação de política de exclusão concluída");

            repo.deleteById(id);

            log.info("Usuário removido com sucesso. ID: {}", id);
            log.debug("Finalizando execute");
        } catch (Exception e) {
            log.error("Erro ao remover usuário. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }
}