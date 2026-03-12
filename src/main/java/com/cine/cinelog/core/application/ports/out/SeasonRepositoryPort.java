package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.core.domain.model.Season;
import java.util.List;
import java.util.Optional;

/**
 * Porta de repositório para operações relacionadas a temporadas.
 * Define as operações CRUD básicas para gerenciar entidades Season.
 */
public interface SeasonRepositoryPort {
    /**
     * Salva uma temporada no repositório.
     *
     * @param season O objeto Season a ser salvo.
     * @return A temporada salva.
     */
    Season save(Season season);

    /**
     * Recupera uma temporada existente do repositório.
     *
     * @param id O ID da temporada a ser recuperada.
     * @return Um Optional contendo a temporada encontrada, ou vazio se não
     *         encontrado.
     */
    Optional<Season> findById(Long id);

    /**
     * Recupera todas as temporadas existentes do repositório.
     *
     * @return Uma lista de temporadas encontradas.
     */
    PageResult<Season> findAll(PageQuery query);

    /**
     * Remove uma temporada existente do repositório.
     *
     * @param id O ID da temporada a ser removida.
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
     * Verifica se uma temporada existe para uma mídia e número de temporada
     * específicos.
     *
     * @param mediaId
     * @param seasonNumber
     * @return
     */
    boolean existsByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber);

    /**
     * Verifica se uma temporada existe pelo seu ID.
     *
     * @param id
     * @return
     */
    boolean existsById(Long id);

    /**
     * Busca uma temporada pelo ID da mídia e número de temporada.
     *
     * @param mediaId      O ID da mídia.
     * @param seasonNumber O número da temporada.
     * @return Um Optional contendo a temporada encontrada, ou vazio.
     */
    Optional<Season> findByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber);
}
