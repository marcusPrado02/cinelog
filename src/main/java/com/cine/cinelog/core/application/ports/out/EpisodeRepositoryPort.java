package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.core.domain.model.Episode;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de persistência relacionadas a episódios.
 * Define as operações CRUD básicas para gerenciar entidades Episode.
 */
public interface EpisodeRepositoryPort {
    /**
     * Salva um episódio no repositório.
     *
     * @param episode O objeto Episode a ser salvo.
     * @return O episódio salvo.
     */
    Episode save(Episode episode);

    /**
     * Recupera um episódio existente do repositório.
     *
     * @param id O ID do episódio a ser recuperado.
     * @return Um Optional contendo o episódio encontrado, ou vazio se não
     *         encontrado.
     */
    Optional<Episode> findById(Long id);

    /**
     * Recupera todos os episódios existentes do repositório.
     *
     * @return Uma lista de episódios encontrados.
     */
    PageResult<Episode> findAll(PageQuery query);

    /**
     * Remove um episódio existente do repositório.
     *
     * @param id O ID do episódio a ser removido.
     */
    void deleteById(Long id);

    /**
     * Verifica se uma entrada de visualização existe para uma mídia específica.
     *
     * @param mediaId O ID da mídia.
     * @return true se existir, false caso contrário.
     */
    boolean existsByMediaId(Long mediaId);

    /**
     * Verifica se um episódio existe pelo ID.
     *
     * @param id O ID do episódio a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsById(Long id);

    /**
     * Verifica se um episódio existe para uma temporada e número de episódio
     * específicos.
     * 
     * @param seasonId
     * @param episodeNumber
     * @return
     */
    boolean existsBySeasonIdAndEpisodeNumber(Long seasonId, Integer episodeNumber);

    /**
     * Verifica se um episódio existe para uma temporada específica.
     * 
     * @param seasonId
     * @return
     */
    boolean existsBySeasonId(Long seasonId);
}