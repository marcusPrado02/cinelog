package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.CreateGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Serviço responsável por criar novos gêneros de mídia no sistema.
 * 
 * <p>
 * Este caso de uso permite cadastrar novos gêneros que podem ser associados
 * a mídias (filmes e séries), como Ação, Drama, Comédia, Terror, etc.
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link CreateGenreUseCase} e utilizando a porta de saída
 * {@link GenreRepositoryPort}
 * para persistência dos dados.
 * 
 * @since 1.0
 * @see CreateGenreUseCase
 * @see GenreRepositoryPort
 * @see Genre
 */
@Transactional
public class CreateGenreService implements CreateGenreUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateGenreService.class);

    private final GenreRepositoryPort repo;

    public CreateGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a criação de um novo gênero no sistema.
     * 
     * @param genre o gênero a ser criado, contendo o nome
     * @return o gênero criado e persistido, com ID gerado
     * 
     * @Observed cria um span no distributed tracing (Tempo) para rastrear esta
     *           operação
     * @Measured registra timing da operação para métricas de performance
     */
    @Override
    @Observed(name = "genre.create", contextualName = "create-genre-service")
    @Measured("cinelog.service.genre.create")
    public Genre execute(Genre genre) {
        log.debug("Iniciando criação de gênero no service. Dados: {}",
                Map.of("name", genre.getName()));

        try {
            // Validação de negócio: nome não pode ser vazio
            if (genre.getName() == null || genre.getName().isBlank()) {
                log.warn("Tentativa de criar gênero com nome vazio ou nulo");
                throw new IllegalArgumentException("Nome do gênero não pode ser vazio");
            }

            // Persistindo no banco de dados
            log.debug("Persistindo gênero no banco de dados. Nome: {}", genre.getName());
            Genre saved = repo.save(genre);

            log.info("Gênero criado com sucesso no service. ID: {}, Nome: {}",
                    saved.getId(), saved.getName());

            return saved;

        } catch (IllegalArgumentException e) {
            // Erros de validação (não precisa stacktrace completo)
            log.warn("Erro de validação ao criar gênero: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // Erros inesperados (loga stacktrace completo)
            log.error("Erro inesperado ao criar gênero no service. Nome: {}. Erro: {}",
                    genre.getName(), e.getMessage(), e);
            throw e;
        }
    }
}