package com.cine.cinelog.core.application.ports.out;

import java.util.List;
import java.util.Optional;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.MediaWithRating;

/**
 * Porta de saída para persistência de `Media`.
 *
 * Implementações desta interface (adapters) são responsáveis por salvar,
 * recuperar e pesquisar entidades de mídia em um armazenamento (banco de
 * dados, serviço externo, etc).
 */
public interface MediaRepositoryPort {

    /**
     * Salva uma mídia no repositório.
     *
     * @param media O objeto Media a ser salvo.
     * @return A mídia salva.
     */
    Media save(Media media);

    /**
     * Recupera uma mídia existente do repositório.
     *
     * @param id O ID da mídia a ser recuperada.
     * @return Um Optional contendo a mídia encontrada, ou vazio se não
     *         encontrado.
     */
    Optional<Media> findById(Long id);

    /**
     * Recupera todas as mídias existentes do repositório.
     *
     * @return Uma lista de mídias encontradas.
     */
    void deleteById(Long id);

    /**
     * Pesquisa mídias com base em critérios fornecidos.
     *
     * @param query O objeto MediaQuery contendo os critérios de pesquisa.
     * @return Um PageResult contendo as mídias encontradas e informações de
     *         paginação.
     */
    PageResult<Media> search(MediaSearchCriteria criteria, PageQuery pageQuery);

    /**
     * Recupera todas as mídias existentes do repositório com paginação.
     *
     * @param query O objeto PageQuery contendo os parâmetros de paginação.
     * @return Um PageResult contendo as mídias encontradas e informações de
     *         paginação.
     */
    PageResult<Media> listAll(PageQuery query);

    /**
     * Verifica se uma mídia existe pelo ID.
     *
     * @param id O ID da mídia a ser verificada.
     * @return true se existir, false caso contrário.
     */
    boolean existsById(Long id);

    /**
     * Retorna uma lista de mídias "candidatas" a recomendação
     * (por exemplo, mais assistidas ou por gênero do usuário).
     */
    List<MediaWithRating> findCandidatesForUser(Long userId);

    /**
     * Busca uma mídia pelo ID do TMDB.
     *
     * @param tmdbId o ID da mídia no TMDB
     * @return Optional com a mídia encontrada, ou vazio
     */
    Optional<Media> findByTmdbId(Long tmdbId);

    /**
     * Retorna todas as mídias de um determinado tipo que possuem tmdbId (para
     * importação batch).
     *
     * @param type tipo da mídia (MOVIE, SERIES)
     * @return lista de mídias com tmdbId não nulo
     */
    List<Media> findAllWithTmdbId(MediaType type);

    /**
     * Retorna todas as mídias que possuem tmdbId mas estão sem imagens (poster ou
     * backdrop).
     *
     * @return lista de mídias com tmdbId não nulo e poster_url ou backdrop_url
     *         nulos
     */
    List<Media> findAllMissingImages();

    /**
     * Retorna todas as midias sem tmdbId definido (ex: dados de seed).
     * Usadas pelo job de vinculacao TMDB para enriquecer registros legados.
     */
    List<Media> findAllWithoutTmdbId();
}
