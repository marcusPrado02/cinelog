package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.GetGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar um gênero específico por seu identificador.
 * 
 * <p>
 * Este caso de uso recupera os dados de um gênero cadastrado,
 * utilizando cache para otimizar performance em consultas repetidas.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Resultado cacheado com chave baseada no ID</li>
 * <li>Lança exceção de domínio caso o gênero não seja encontrado</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link GetGenreUseCase} e utilizando a porta de saída
 * {@link GenreRepositoryPort}.
 * 
 * @since 1.0
 * @see GetGenreUseCase
 * @see GenreRepositoryPort
 */
@Transactional(readOnly = true)
public class GetGenreService implements GetGenreUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetGenreService.class);

    private final GenreRepositoryPort repo;

    public GetGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca um gênero por seu identificador único.
     * 
     * @param id o identificador único do gênero a ser buscado
     * @return o gênero encontrado
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         gênero não existir
     */
    @Observed(name = "genre.get", contextualName = "get-genre-service")
    @Measured("cinelog.service.genre.get")
    @AlertIfSlow(thresholdMs = 500)
    @Cacheable(value = "genreById", key = "#id")
    @Override
    public Genre execute(Long id) {
        log.debug("Iniciando busca de gênero no service. ID: {}", id);

        try {
            Genre genre = repo.findById(id).orElseThrow(() -> {
                log.warn("Gênero não encontrado. ID: {}", id);
                return DomainException.of(ErrorCode.GEN_NOT_FOUND, "Genre not found: " + id);
            });

            log.debug("Gênero encontrado. ID: {}, Nome: {}", genre.getId(), genre.getName());
            return genre;

        } catch (DomainException e) {
            // Já foi logado acima
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar gênero. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}