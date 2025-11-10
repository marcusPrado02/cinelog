package com.cine.cinelog.core.application.ports.out;

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
    List<Season> findAll();

    /**
     * Remove uma temporada existente do repositório.
     *
     * @param id O ID da temporada a ser removida.
     */
    void deleteById(Long id);
}