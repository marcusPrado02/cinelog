package com.cine.cinelog.core.application.ports.out;

import java.util.List;
import java.util.Optional;

import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;
import com.cine.cinelog.core.domain.model.tmdb.TmdbDiscoverQuery;
import com.cine.cinelog.core.domain.model.tmdb.TmdbGenre;
import com.cine.cinelog.core.domain.model.tmdb.TmdbImageConfig;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSearchResult;
import com.cine.cinelog.core.domain.model.tmdb.TmdbPersonDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewsPage;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSeasonDetails;

/**
 * Porta de saída para integração com o TMDb (The Movie Database API).
 *
 * <p>
 * Esta interface abstrai os endpoints REST do TMDb em operações orientadas ao
 * domínio do CineLog, permitindo que a aplicação:
 * <ul>
 * <li>Busque detalhes de filmes e séries</li>
 * <li>Realize buscas textuais (search)</li>
 * <li>Descubra conteúdos por filtros (discover)</li>
 * <li>Sincronize gêneros, créditos e configuração de imagens</li>
 * </ul>
 *
 * <p>
 * Segue o padrão de Arquitetura Hexagonal: o domínio conhece somente este
 * contrato, e a implementação HTTP fica na camada de infraestrutura.
 */
public interface TmdbClientPort {

    // ================== Detalhes de mídia ==================

    /**
     * Busca uma mídia (filme ou série) pelo ID do TMDb.
     *
     * A implementação deve tentar:
     * <ul>
     * <li>/movie/{id}</li>
     * <li>/tv/{id}</li>
     * </ul>
     * retornando o primeiro match encontrado.
     *
     * @param tmdbId identificador da mídia no TMDb
     * @return Optional com os detalhes, ou vazio se não encontrada
     */
    Optional<TmdbMediaDetails> fetchByTmdbId(Long tmdbId);

    /**
     * Busca detalhes de um filme pelo ID do TMDb usando /movie/{id}.
     *
     * @param tmdbId identificador TMDb do filme
     * @return Optional com os detalhes, ou vazio se não encontrado
     */
    Optional<TmdbMediaDetails> fetchMovie(Long tmdbId);

    /**
     * Busca detalhes de uma série pelo ID do TMDb usando /tv/{id}.
     *
     * @param tmdbId identificador TMDb da série
     * @return Optional com os detalhes, ou vazio se não encontrada
     */
    Optional<TmdbMediaDetails> fetchTv(Long tmdbId);

    // ================== Busca textual (search) ==================

    /**
     * Busca uma única mídia (filme ou série) por título + ano, retornando
     * apenas o resultado mais relevante.
     *
     * <p>
     * A implementação pode usar:
     * <ul>
     * <li>/search/movie</li>
     * <li>/search/tv</li>
     * </ul>
     * e aplicar uma estratégia simples de seleção do melhor resultado.
     *
     * @param title título da mídia
     * @param year  ano de lançamento (opcional)
     * @return Optional com mídia mais relevante, ou vazio
     */
    Optional<TmdbMediaDetails> searchByTitleAndYear(String title, Integer year);

    /**
     * Realiza uma busca textual por filmes, retornando uma página de
     * resultados resumidos. Mapeia para GET /search/movie.
     *
     * @param query termo de busca
     * @param year  ano para filtrar (opcional)
     * @param page  página desejada (>= 1)
     * @return Resultado paginado com resumos de filmes
     */
    TmdbSearchResult<TmdbMediaSummary> searchMovies(String query, Integer year, int page);

    /**
     * Realiza uma busca textual por séries, retornando uma página de
     * resultados resumidos. Mapeia para GET /search/tv.
     *
     * @param query termo de busca
     * @param year  ano de primeira exibição (opcional)
     * @param page  página desejada (>= 1)
     * @return Resultado paginado com resumos de séries
     */
    TmdbSearchResult<TmdbMediaSummary> searchTvShows(String query, Integer year, int page);

    // ================== Discover ==================

    /**
     * Descobre filmes usando filtros do TMDb (gêneros, ano, ordenação, etc.).
     * Mapeia para GET /discover/movie.
     *
     * @param query filtros de discover
     * @return Resultado paginado com resumos de filmes
     */
    TmdbSearchResult<TmdbMediaSummary> discoverMovies(TmdbDiscoverQuery query);

    /**
     * Descobre séries usando filtros do TMDb (gêneros, ano, ordenação, etc.).
     * Mapeia para GET /discover/tv.
     *
     * @param query filtros de discover
     * @return Resultado paginado com resumos de séries
     */
    TmdbSearchResult<TmdbMediaSummary> discoverTvShows(TmdbDiscoverQuery query);

    // ================== Gêneros ==================

    /**
     * Obtém a lista de gêneros de filmes do TMDb. Mapeia para GET
     * /genre/movie/list.
     *
     * @return lista de gêneros de filme
     */
    List<TmdbGenre> fetchMovieGenres();

    /**
     * Obtém a lista de gêneros de séries do TMDb. Mapeia para GET /genre/tv/list.
     *
     * @return lista de gêneros de série
     */
    List<TmdbGenre> fetchTvGenres();

    // ================== Créditos (cast & crew) ==================

    /**
     * Busca créditos (cast & crew) de um filme. Mapeia para GET
     * /movie/{id}/credits.
     *
     * @param movieId identificador TMDb do filme
     * @return Optional com créditos, ou vazio se não encontrado
     */
    Optional<TmdbCredits> fetchMovieCredits(Long movieId);

    /**
     * Busca créditos (cast & crew) de uma série. Mapeia para GET /tv/{id}/credits.
     *
     * @param tvId identificador TMDb da série
     * @return Optional com créditos, ou vazio se não encontrado
     */
    Optional<TmdbCredits> fetchTvCredits(Long tvId);

    // ================== Configuração (imagens, etc.) ==================

    /**
     * Busca a configuração global de imagens do TMDb, utilizada para montar
     * URLs de poster/backdrop de forma dinâmica. Mapeia para GET /configuration.
     *
     * @return configuração de imagens
     */
    TmdbImageConfig fetchImageConfig();

    // ================== Temporadas ==================

    /**
     * Busca detalhes de uma temporada de série. Mapeia para GET
     * /tv/{id}/season/{season_number}.
     *
     * @param tvId         identificador TMDb da série
     * @param seasonNumber número da temporada
     * @return detalhes da temporada, ou vazio se não encontrado
     */
    Optional<TmdbSeasonDetails> fetchTvSeason(Long tvId, int seasonNumber);

    // ================== Reviews ==================

    /**
     * Busca reviews de um filme do TMDb (GET /movie/{id}/reviews).
     *
     * @param movieTmdbId ID TMDB do filme
     * @param page        página (1-based)
     * @return página de reviews
     */
    TmdbReviewsPage fetchMovieReviews(Long movieTmdbId, int page);

    /**
     * Busca reviews de uma série do TMDb (GET /tv/{id}/reviews).
     *
     * @param tvTmdbId ID TMDB da série
     * @param page     página (1-based)
     * @return página de reviews
     */
    TmdbReviewsPage fetchTvReviews(Long tvTmdbId, int page);

    // ================== Detalhes de Pessoa ==================

    /**
     * Busca detalhes completos de uma pessoa (GET /person/{id}).
     *
     * @param tmdbPersonId ID TMDB da pessoa
     * @return Optional com detalhes, ou vazio se não encontrada
     */
    Optional<TmdbPersonDetails> fetchPersonDetails(Long tmdbPersonId);
}
