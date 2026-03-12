package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.UpdateGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por atualizar os dados de um gênero existente.
 *
 * <p>
 * Este caso de uso permite alterar o nome de um gênero já cadastrado.
 *
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link UpdateGenreUseCase} e utilizando a porta de saída
 * {@link GenreRepositoryPort}.
 *
 * @since 1.0
 * @see UpdateGenreUseCase
 * @see GenreRepositoryPort
 */
@Transactional
public class UpdateGenreService implements UpdateGenreUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateGenreService.class);

    private final GenreRepositoryPort repo;

    public UpdateGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a atualização de um gênero existente.
     *
     * @param id    o identificador único do gênero a ser atualizado
     * @param genre os novos dados do gênero (nome)
     * @return o gênero atualizado e persistido
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         gênero não existir
     */
    @Override
    @Observed(name = "genre.update", contextualName = "update-genre-service")
    @Measured("cinelog.service.genre.update")
    @AuditableAction(module = "GENRE", action = "UPDATE", description = "Atualização de gênero")
    @Caching(evict = {
            @CacheEvict(value = "genresPage", allEntries = true),
            @CacheEvict(value = "genreById", key = "#id")
    })
    public Genre execute(Long id, Genre genre) {
        log.debug("Iniciando atualização de gênero no service. ID: {}, Novo nome: {}", id, genre.getName());

        try {
            var existing = repo.findById(id).orElseThrow(() -> {
                log.warn("Gênero não encontrado para atualização. ID: {}", id);
                return DomainException.of(ErrorCode.GEN_NOT_FOUND, "Genre not found: " + id);
            });

            existing.setName(genre.getName());
            Genre saved = repo.save(existing);

            log.info("Gênero atualizado com sucesso. ID: {}, Nome: {}", saved.getId(), saved.getName());
            return saved;

        } catch (DomainException e) {
            log.warn("Erro de domínio ao atualizar gênero. ID: {}, Erro: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar gênero. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
