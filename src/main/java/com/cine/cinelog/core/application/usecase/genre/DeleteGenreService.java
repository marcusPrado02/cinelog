package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.DeleteGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por excluir um gênero do sistema.
 * 
 * <p>
 * Remove um gênero cadastrado. Note que a exclusão pode falhar
 * se houver mídias associadas ao gênero (integridade referencial
 * gerenciada pela camada de persistência).
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link DeleteGenreUseCase} e utilizando a porta de saída
 * {@link GenreRepositoryPort}.
 * 
 * @since 1.0
 * @see DeleteGenreUseCase
 * @see GenreRepositoryPort
 */
@Transactional
public class DeleteGenreService implements DeleteGenreUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteGenreService.class);

    private final GenreRepositoryPort repo;

    public DeleteGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a exclusão de um gênero do sistema.
     * 
     * @param id o identificador único do gênero a ser excluído
     * @throws org.springframework.dao.DataIntegrityViolationException se houver
     *                                                                 mídias
     *                                                                 associadas ao
     *                                                                 gênero
     */
    @Override
    @Observed(name = "genre.delete", contextualName = "delete-genre-service")
    @Measured("cinelog.service.genre.delete")
    @AuditableAction(module = "GENRE", action = "DELETE", description = "Exclusão de gênero")
    @SecureOperation(module = "GENRE", value = "CONTENT_ADMIN")
    public void execute(Long id) {
        log.debug("Iniciando exclusão de gênero no service. ID: {}", id);

        try {
            repo.deleteById(id);
            log.info("Gênero excluído com sucesso. ID: {}", id);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Erro de integridade ao excluir gênero. ID: {}, Erro: Existem mídias associadas", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir gênero. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}